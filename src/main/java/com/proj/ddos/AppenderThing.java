package com.proj.ddos;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

@Plugin(name = "AppenderThing", category = "Core", elementType = "appender", printObject = true)
public final class AppenderThing extends AbstractAppender {

    protected AppenderThing(String name, Filter filter, Layout<? extends Serializable> layout,
            boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions, null);
    }

    private static TextFlow textFlow;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();

    @Override
    public void append(LogEvent event) {
        readLock.lock();

        final String message = new String(getLayout().toByteArray(event));

        try {
            Platform.runLater(() -> {
                try {
                    if (textFlow != null) {
                        Text text = new Text(message);
                        Color color = Color.BLACK;
                        switch (message.split("-")[0]) {
                            case "INFO":
                                color = Color.GREEN;
                                break;
                            case "WARN":
                                color = Color.PINK;
                                break;
                            case "ERROR":
                                color = Color.RED;
                                break;
                            default:
                                break;
                        }
                        text.setFill(color);
                        textFlow.getChildren().add(text);

                    } else {
                        System.out.println("NULL TextFlow");
                    }
                } catch (final Throwable t) {
                    System.out.println("Error while append to TextFlow: " + t.getMessage());
                    t.printStackTrace();
                }
            });
        } catch (final IllegalStateException ex) {
            ex.printStackTrace();

        } finally {
            readLock.unlock();
        }
    }

    @PluginFactory
    public static AppenderThing createAppender(@PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter) {
        if (name == null) {
            LOGGER.error("No name provided for TextAreaAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new AppenderThing(name, filter, layout, true);
    }

    public static void setOutputArea(TextFlow textFlow) {
        AppenderThing.textFlow = textFlow;
    }
}