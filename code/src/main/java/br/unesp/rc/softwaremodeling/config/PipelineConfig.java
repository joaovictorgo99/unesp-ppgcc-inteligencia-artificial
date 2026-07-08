package br.unesp.rc.softwaremodeling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.models.Gemini;
import com.google.adk.runner.InMemoryRunner;

/**
 * Configures the Gemini model, agents, and pipeline used for software modeling automation.
 * 
 * <p>The pipeline runs sequentially: the {@code extractingAgent} extracts requirements
 * from a requirements specification document, the {@code modelingAgent} generates 
 * PlantUML codes from those requirements, and the {@code evaluatingAgent} evaluates 
 * the generated code and the pipeline execution.
 * 
 * @author João Víctor Gomes de Oliveira
 */
@Configuration
public class PipelineConfig {
    /**
     * Defines the Gemini model used by all agents in the pipeline.
     * 
     * @return the configured {@link Gemini} model
     */
    @Bean
    public Gemini geminiModel() {
        return Gemini.builder()
                .modelName("gemini-model")
                .apiKey("api-key")
                .build();
    }

    /**
     * Builds an extracting agent.
     * 
     * <p>Reads a requirements specification document and outputs the 
     * functional and non-functional requirements.
     * 
     * @param geminiModel the Gemini model
     * @return the extracting agent
     */
    @Bean
    public LlmAgent extractingAgent(Gemini geminiModel) {
        return LlmAgent.builder()
                .model(geminiModel)
                .name("ExtractingAgent")
                .description("Extract functional and non-functional requirements based on a requirements specification document.")
                .instruction("""
                    You are a requirements extractor.
                    Based *only* on the requirements specification document, extract the functional and non-functional requirements.
                    Output *only* the requirements as a concise, bulleted list. Focus on the most important points.
                    Do not add any other text before or after the requirements list.
                """)
                .outputKey("generated_requirements")
                .build();
    }

    /**
     * Builds a modeling agent.
     * 
     * <p>Reads the requirements from {@link #extractingAgent(Gemini)} and outputs the 
     * PlantUML code for a use case diagram and a class diagram.
     * 
     * @param geminiModel the Gemini model
     * @return the modeling agent
     */
    @Bean
    public LlmAgent modelingAgent(Gemini geminiModel) {
        return LlmAgent.builder()
                .model(geminiModel)
                .name("ModelingAgent")
                .description("Write PlantUML code based on the extracted requirements.")
                .instruction("""
                    You are a PlantUML code generator.
                    Based *only* on the provided requirements: {generated_requirements}, write PlantUML code.
                    The first is a use case diagram, and the second is a class diagram.
                    Output *only* the complete PlantUML code, enclosed in triple backticks (```plantuml ... ```).
                    Do not add any other text before or after the code block.
                """)
                .outputKey("generated_code")
                .build();
    }

    /**
     * Builds an evaluating agent.
     * 
     * <p>Reads the PlantUML code from {@link #modelingAgent(Gemini)} and outputs the code
     * evaluation, along with the steps and decisions made throughout the pipeline
     * execution.
     * 
     * @param geminiModel the Gemini model
     * @return the evaluating agent
     */
    @Bean
    public LlmAgent evaluatingAgent(Gemini geminiModel) {
        return LlmAgent.builder()
                .model(geminiModel)
                .name("EvaluatingAgent")
                .description("Evaluate the PlantUML code and the pipeline execution.")
                .instruction("""
                    You are a PlantUML code evaluator.
                    Based *only* on the provided PlantUML code: {generated_code}, evaluate the code.
                    After that, list the steps and decisions made during the modeling pipeline execution.
                    Output *only* the evaluation as a plain text.
                    Do not add any other text before or after the code block.
                """)
                .outputKey("refactored_code")
                .build();
    }

    /**
     * Builds a sequential agentic pipeline for software modeling automation.
     * 
     * @param extractingAgent the extracting subagent
     * @param modelingAgent the modeling subagent
     * @param evaluatingAgent the evaluating subagent
     * @return the sequential agentic pipeline
     */
    @Bean
    public SequentialAgent softwareModelingPipelineAgent(LlmAgent extractingAgent, LlmAgent modelingAgent, LlmAgent evaluatingAgent) {
        return SequentialAgent.builder()
                .name("SoftwareModelingPipelineAgent")
                .description("Creates two PlantUML codes: use case diagrams and class diagrams.")
                .subAgents(extractingAgent, modelingAgent, evaluatingAgent)
                .build();
    }

    /**
     * Builds an in-memory runner to execute the sequential agentic pipeline.
     * 
     * @param softwareModelingPipelineAgent the sequential agentic pipeline to run
     * @return the in-memory runner for the sequential agentic pipeline
     */
    @Bean
    public InMemoryRunner inMemoryRunner(SequentialAgent softwareModelingPipelineAgent) {
        return new InMemoryRunner(softwareModelingPipelineAgent, "SoftwareModelingPipelineAgent");
    }
}
