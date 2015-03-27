package chatapp;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.Callable;

/**
 * Created by The Joshis on 10/5/2014.
 */
public class AudioCapture {
    Stage owner;

    AudioCapture(Stage ownerStage) {
        owner = ownerStage;
    }

    public byte[] show() throws FileNotFoundException, IOException {
        final File file = new File(System.getProperty("user.home") +
                File.separator +
                "Chat Files" +
                File.separator +
                "temp.wav");
        byte[] recordedAudio = null;
        final Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);
        VBox vBox = new VBox(10);
        HBox hBox = new HBox(10);
        vBox.setPadding(new Insets(10));

        Button recordButton = new Button("Record");
        Button stopButton = new Button("Stop");
        final Text text = new Text();

        final AudioThread thread = new AudioThread();

        recordButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                text.setText("Recording..");
                thread.start();
            }
        });

        stopButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                thread.finish();
                dialogStage.close();
            }
        });

        hBox.getChildren().addAll(recordButton, stopButton);
        vBox.getChildren().addAll(text, hBox);

        Scene scene = new Scene(vBox);
        dialogStage.setScene(scene);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();

        byte[] data = Files.readAllBytes(file.toPath());
        file.delete();
        return data;
    }

    public class AudioThread extends Thread {
        TargetDataLine line;

        @Override
        public void run() {
            File wavFile = new File(System.getProperty("user.home") +
                    File.separator +
                    "Chat Files" +
                    File.separator +
                    "temp.wav"
            );

            try {
                AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

                AudioFormat format = getAudioFormat();
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    System.exit(0);
                }

                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                AudioInputStream audioInputStream = new AudioInputStream(line);

                AudioSystem.write(audioInputStream, fileType, wavFile);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        AudioFormat getAudioFormat() {
            float sampleRate = 16000;
            int sampleSizeInBits = 8;
            int channels = 2;
            boolean signed = true;
            boolean bigEndian = true;
            AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits,
                    channels, signed, bigEndian);
            return format;
        }

        void finish() {
            line.stop();
            line.close();
        }


    }
}
