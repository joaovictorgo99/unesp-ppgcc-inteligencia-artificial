package br.unesp.rc.softwaremodeling.runner;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;

import io.reactivex.rxjava3.core.Flowable;

/**
 * Executes the software modeling pipeline on application startup.
 *
 * <p>Reads a requirements specification document, sends it to the 
 * {@link InMemoryRunner}, and prints the final generated content to 
 * the console.
 *
 * @author João Víctor Gomes de Oliveira
 */
@Component
public class PipelineRunner implements CommandLineRunner {
    private static final String USER_ID = "test_user_123";
    private final InMemoryRunner runner;

    /**
     * Creates a pipeline runner backed by the given in-memory runner.
     * 
     * @param runner the in-memory runner used to execute the pipeline
     */
    public PipelineRunner(InMemoryRunner runner) {
        this.runner = runner;
    }

    /**
     * Runs the software modeling pipeline for a single requirements specification document.
     * 
     * <p>Creates a new session, sends the document along with a fixed prompt to 
     * the pipeline, and prints the final response once the event stream completes.
     * 
     * @param args command-line arguments (not used)
     * @throws Exception if the document cannot be read or the pipeline execution fails
     */
    @Override
    public void run(String... args) throws Exception {
        byte[] pdfBytes = Files.readAllBytes(Path.of("resources/documents/document.pdf"));
        String prompt = "Write two PlantUML codes, based on the requirements specification document, the first is a use case diagram and the second is a class diagram.";
        Session session = runner.sessionService().createSession("SoftwareModelingPipelineAgent", USER_ID).blockingGet();
        Content userMessage = Content.fromParts(Part.fromBytes(pdfBytes, "application/pdf"), Part.fromText(prompt));
        Flowable<Event> eventStream = runner.runAsync(USER_ID, session.id(), userMessage);

        eventStream.blockingForEach(event -> {
            if (event.finalResponse()) {
                System.out.println(event.stringifyContent());
            }
        });
    }
}
