#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;

public class ${NAME} extends Cog {
   public ${NAME}(Bot bot) {
       super(bot);
   }

   public String getName() {
       return "${NAME}";
   }

   public String getDescription() {
       return "A description.";
   }
   
   @Command(name="temp", desc="")
   public void cmdTemp(Context ctx) {
       
   }
}
