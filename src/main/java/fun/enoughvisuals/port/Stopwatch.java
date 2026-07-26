package fun.enoughvisuals.port;

import lombok.Generated;

public class Stopwatch {
   private long timestamp;

   public Stopwatch() {
      this.invoke();
   }

   public boolean check(double d) {
      return System.currentTimeMillis() - d >= this.timestamp;
   }

   public boolean check2(double d) {
      boolean flag = this.check(d);
      if (flag) {
         this.invoke();
      }

      return flag;
   }

   public void invoke() {
      this.timestamp = System.currentTimeMillis();
   }

   public long compute() {
      return System.currentTimeMillis() - this.timestamp;
   }

   public void setTimestamp(long l) {
      this.timestamp = System.currentTimeMillis() - l;
   }

   @Generated
   public long getTimestamp() {
      return this.timestamp;
   }
}
