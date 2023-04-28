package blog.persistence.config

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import fusion.json.jackson.CborSerializable

object ConfigEntity {
  case class ConfigState(namespace: String, dataId: String, configType: String, content: String)

  sealed trait Command extends CborSerializable
  sealed trait Event extends CborSerializable

  final case class Query(configType: Option[String], replyTo: ActorRef[Option[ConfigState]]) extends Command

  final case class State(config: Option[ConfigState] = None) extends CborSerializable

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("ConfigEntity")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(Entity(TypeKey)(entityContext => apply(entityContext.entityId)))

  private def apply(entityId: String): Behavior[Command] = {
    val Array(namespace, dataId) = entityId.split('@')
    Behaviors.setup(context => new ConfigEntity(namespace, dataId, context).eventSourcedBehavior())
  }
}

import ConfigEntity._
class ConfigEntity private (namespace: String, dataId: String, context: ActorContext[Command]) {
  def eventSourcedBehavior(): EventSourcedBehavior[Command, Event, State] =
    EventSourcedBehavior(PersistenceId.of(TypeKey.name, dataId), State(), commandHandler, eventHandler)

  def commandHandler(state: State, command: Command): Effect[Event, State] = command match {
    case Query(configType, replyTo) =>
      state.config match {
        case None =>
          Effect.reply(replyTo)(None)
        case Some(config) =>
          val resp = if (configType.forall(v => config.configType.contains(v))) Some(config) else None
          Effect.reply(replyTo)(resp)
      }
  }

  def eventHandler(state: State, event: Event): State = ???
}
