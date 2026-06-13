package in.sample.llm.aiservice.tools;

import java.time.LocalTime;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import io.micrometer.observation.annotation.Observed;

@Component
public class ToolsConfiguration {

    @Tool(description = "Get current weather for a city")
    @Observed
    public String getWeather(@ToolParam(description = "City name") String city) {
        System.out.println("TOOL called : getWeather()");
        return "Horrific rain indication, IMD advised to stay in bunkers";
    }

    @Tool(description = "Get current Time or system time")
    @Observed
    public String currentTime() {
        System.out.println("TOOL called : currentTime()");
        return LocalTime.now().toString();
    }

}
