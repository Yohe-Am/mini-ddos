package com.proj.ddos;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrimaryController {
    @FXML
    private TextField urlField;
    @FXML
    private Button goButton;
    @FXML
    private Text threadCountText;
    @FXML
    private Text requestCountText;
    @FXML
    private Text statusText;
    @FXML
    private Text serverStatusText;
    @FXML
    private Button stopButton;
    @FXML
    private TextFlow logTextFlow;
    @FXML
    private ScrollPane logScrollPane;

    private static final Logger LOGGER = LogManager.getRootLogger();
    private RequestManager manager;

    public PrimaryController() {
    }

    @FXML
    private void initialize() {
        goButton.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                go();
            }
        });
        stopButton.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                stop();
            }
        });
        AppenderThing.setOutputArea(logTextFlow);
        urlField.setText("http://localhost:8000");
    }

    private void go() {

        String address = urlField.getText();
        if (!isSaneURL(address)) {
            LOGGER.error("We can't parse that address");
            return;
        }
        manager = new RequestManager(URI.create(address));
        if (!manager.checkAvailability()) {
            LOGGER.error("Is your server running? We don't think so.");
            return;
        }

        manager.getIsRunningProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> value, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    statusText.setText("running");
                    goButton.setDisable(true);
                } else {
                    statusText.setText("stopped");
                    goButton.setDisable(false);
                }
            }

        });

        manager.getThreadCountProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> value, Number oldValue, Number newValue) {
                threadCountText.setText(String.valueOf(newValue));
            }

        });

        RequestThread.requestCounter.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> value, Number oldValue, Number newValue) {
                requestCountText.setText(String.valueOf(newValue));
            }
        });

        logTextFlow.getChildren().addListener(new ListChangeListener<Node>() {

            @Override
            public void onChanged(Change<? extends Node> arg0) {
                if (logScrollPane != null) {
                    logTextFlow.layout();
                    logScrollPane.layout();
                    logScrollPane.setVvalue(1.0f);
                }
            }

        });

        manager.start();
    }

    private void stop() {
        if (manager != null && manager.getIsRunningProperty().getValue()) {
            manager.stop();
        }
    }

    private boolean isSaneURL(String address) {
        Pattern pattern = Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");
        Matcher matcher = pattern.matcher(address);
        return matcher.matches();
    }

}
