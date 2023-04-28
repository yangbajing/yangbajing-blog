package blog.oauth2.peraccesstoken

import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import blog.oauth2.{ AccessToken, OAuthUtils }

object RefreshTokenEntity {
  sealed trait Command
  final case class Create(refreshToken: String, replyTo: ActorRef[AccessToken]) extends Command

  sealed trait Event
  final case class Created(refreshToken: String) extends Event

  final case class State(refreshToken: String = "", expiresEpochMillis: Long = 0L)

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("RefreshTokenEntity")
  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(Entity(TypeKey)(ec => apply(ec)))

  private def apply(ec: EntityContext[Command]): Behavior[Command] =
    Behaviors.setup(
      context =>
        EventSourcedBehavior[Command, Event, State](
          PersistenceId.of(ec.entityTypeKey.name, ec.entityId),
          State(),
          (state, command) => commandHandler(context, state, command),
          (state, event) => eventHandler(state, event)))

  private def commandHandler(context: ActorContext[Command], state: State, command: Command): Effect[Event, State] =
    command match {
      case Create(refreshToken, replyTo) =>
        Effect.persist(Created(refreshToken)).thenRun { _ =>
          val entityRef =
            ClusterSharding(context.system).entityRefFor(AccessTokenEntity.TypeKey, OAuthUtils.generateToken())
          entityRef ! AccessTokenEntity.Create(refreshToken, replyTo)
        }
    }

  private def eventHandler(state: State, event: Event): State = event match {
    case Created(refreshToken) => State(refreshToken, Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli)
  }
}
