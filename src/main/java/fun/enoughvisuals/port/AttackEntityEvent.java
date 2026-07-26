package fun.enoughvisuals.port;

import net.minecraft.entity.Entity;

public class AttackEntityEvent extends Event {
   private Entity entity;

   public Entity getEntity() {
      return this.entity;
   }

   public void setEntity(Entity entity) {
      this.entity = entity;
   }

   public AttackEntityEvent(Entity entity) {
      this.entity = entity;
   }
}
