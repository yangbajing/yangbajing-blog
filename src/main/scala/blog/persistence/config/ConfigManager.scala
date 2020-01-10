package blog.persistence.config

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.cluster.sharding.typed.{ ClusterShardingSettings, ShardingEnvelope }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.stream.scaladsl.{ Sink, Source }
import fusion.json.jackson.CborSerializable
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import blog.persistence.config.ConfigEntity.ConfigState

import scala.util.{ Failure, Success }

object ConfigManager {
  sealed trait Command extends CborSerializable
  sealed trait Event extends CborSerializable
  sealed trait Response extends CborSerializable

  final case class Query(dataId: Option[String], configType: Option[String], page: Int, size: Int) extends Command
  final case class ReplyCommand(in: AnyRef, replyTo: ActorRef[Response]) extends Command
  private final case class InternalResponse(replyTo: ActorRef[Response], response: Response) extends Command

  case class ConfigResponse(status: Int, message: String = "", data: Option[AnyRef] = None) extends Response

  final case class State(dataIds: Vector[String] = Vector()) extends CborSerializable

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("ConfigManager")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(
      Entity(TypeKey)(entityContext => apply(entityContext.entityId))
        .withSettings(ClusterShardingSettings(system).withPassivateIdleEntityAfter(Duration.Zero)))

  def apply(namespace: String): Behavior[Command] =
    Behaviors.setup(context => new ConfigManager(namespace, context).eventSourcedBehavior())
}

import ConfigManager._
class ConfigManager private (namespace: String, context: ActorContext[Command]) {
  private implicit val system = context.system
  private implicit val timeout: Timeout = 5.seconds
  import context.executionContext
  private val configEntity = ConfigEntity.init(context.system)

  def eventSourcedBehavior(): EventSourcedBehavior[Command, Event, State] =
    EventSourcedBehavior(
      PersistenceId.of(TypeKey.name, namespace),
      State(), {
        case (state, ReplyCommand(in, replyTo)) =>
          replyCommandHandler(state, replyTo, in)
        case (_, InternalResponse(replyTo, response)) =>
          Effect.reply(replyTo)(response)
        case _ =>
          Effect.none
      },
      eventHandler)

  def replyCommandHandler(state: State, replyTo: ActorRef[Response], command: AnyRef): Effect[Event, State] =
    command match {
      case in: Query => processPageQuery(state, replyTo, in)
    }

  private def processPageQuery(state: State, replyTo: ActorRef[Response], in: Query): Effect[Event, State] = {
    val offset = if (in.page > 0) (in.page - 1) * in.size else 0
    val responseF = if (offset < state.dataIds.size) {
      Source(state.dataIds)
        .filter(dataId => in.dataId.forall(v => v.contains(dataId)))
        .mapAsync(20) { dataId =>
          configEntity.ask[Option[ConfigState]](replyTo =>
            ShardingEnvelope(s"$namespace@$dataId", ConfigEntity.Query(in.configType, replyTo)))
        }
        .collect { case Some(value) => value }
        .drop(offset)
        .take(in.size)
        .runWith(Sink.seq)
        .map(items => ConfigResponse(IntStatus.OK, data = Some(items)))
    } else {
      Future.successful(ConfigResponse(IntStatus.OK, data = Some(Nil)))
    }
    context.pipeToSelf(responseF) {
      case Success(value) => InternalResponse(replyTo, value)
      case Failure(e)     => InternalResponse(replyTo, ConfigResponse(IntStatus.INTERNAL_ERROR, e.getLocalizedMessage))
    }
    Effect.none
  }

  def eventHandler(state: State, event: Event): State = ???
}
