package blog.oauth2.peraccesstoken

import akka.actor.typed.scaladsl.{ Behaviors, TimerScheduler }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.cluster.sharding.typed.{ ClusterShardingSettings, ShardingEnvelope }
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.persistence.typed.{ PersistenceId, RecoveryCompleted }
import blog.oauth2.{ AccessToken, OAuthUtils }
import fusion.json.jackson.CborSerializable

import scala.concurrent.duration._

object AccessTokenEntity {
  final case class State(accessToken: String, expiresEpochMillis: Long = 0L, refreshToken: String = "")
      extends CborSerializable

  sealed trait Command extends CborSerializable
  case object StopSelf extends Command
  final case class Check(replyTo: ActorRef[Int]) extends Command
  final case class Create(refreshToken: String, replTo: ActorRef[AccessToken]) extends Command

  sealed trait Event extends CborSerializable
  final case class Created(expiresIn: FiniteDuration, refreshToken: String) extends Event

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("AccessTokenEntity")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(
      Entity(TypeKey)(ec => apply(ec.entityId))
        .withSettings(ClusterShardingSettings(system).withPassivateIdleEntityAfter(Duration.Zero)))

  private def apply(accessToken: String): Behavior[Command] =
    Behaviors.setup(context =>
      Behaviors.withTimers { timers =>
        val behavior = EventSourcedBehavior[Command, Event, State](
          PersistenceId.of(TypeKey.name, accessToken),
          State(accessToken),
          (state, command) => commandHandler(timers, state, command),
          (state, event) => eventHandler(state, event))
        behavior.receiveSignal {
          case (state, RecoveryCompleted) =>
            val now = System.currentTimeMillis()
            if (state.expiresEpochMillis < now) context.self ! StopSelf
            else timers.startSingleTimer(StopSelf, (state.expiresEpochMillis - now).millis)
        }
      })

  private def commandHandler(timers: TimerScheduler[Command], state: State, command: Command): Effect[Event, State] =
    command match {
      case Check(replyTo) =>
        if (state.expiresEpochMillis == 0L) {
          Effect.stop().thenReply(replyTo)(_ => 401)
        } else {
          val status = if (System.currentTimeMillis() < state.expiresEpochMillis) 200 else 401
          Effect.reply(replyTo)(status)
        }

      case Create(refreshToken, replTo) =>
        if (state.expiresEpochMillis > 0L) // 返回已存在 AccessToken
          Effect.reply(replTo)(createAccessToken(state))
        else
          Effect.persist(Created(2.hours, refreshToken)).thenReply(replTo) { st =>
            timers.startSingleTimer(StopSelf, (st.expiresEpochMillis - System.currentTimeMillis()).millis)
            createAccessToken(st)
          }

      case StopSelf => Effect.stop()
    }

  @inline private def createAccessToken(state: State) =
    AccessToken(
      state.accessToken,
      (state.expiresEpochMillis - System.currentTimeMillis() - OAuthUtils.DEVIATION) / 1000,
      state.refreshToken)

  private def eventHandler(state: State, event: Event): State = event match {
    case Created(expiresIn, refreshToken) =>
      state.copy(
        expiresEpochMillis = System.currentTimeMillis() + expiresIn.toMillis + OAuthUtils.DEVIATION,
        refreshToken = refreshToken)
  }
}
