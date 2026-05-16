package in.sample.llm.aiservice.tools;

import java.time.LocalTime;

import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.micrometer.observation.annotation.Observed;

@Component
public class ToolsConfiguration {

    @Tool("Get current weather for a city")
    @Observed
    public String getWeather(
      @P("City name") String city) {
      System.out.println("TOOL called : getWeather()");
      return "Horrific rain indication, IMD advised to stay in bunkers";
    }

    @Tool("Get current Time or system time")
    @Observed
    public String currentTime() {
      System.out.println("TOOL called : currentTime()");
        return LocalTime.now().toString();
    }

}
