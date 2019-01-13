package dialogs.exception;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionPane extends BorderPane {

    private final ObjectProperty<Exception> exception = null;

    public TextArea stackTrace;
    public Label message;

    public ObjectProperty<Exception> exceptionProperty() {
        return exception;
    }

    public final Exception getException() {
        return exceptionProperty().get();
    }

    public final void setException(Exception exception) {
        exceptionProperty().set(exception);
    }

    public ExceptionPane() {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("exception.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (Exception e){
            e.printStackTrace();
            return;
        }

        exception.addListener((obs, oldException, newException) -> {
            if (newException == null) {
                message.setText(null);
                stackTrace.setText(null);
            } else {
                message.setText(newException.getMessage());
                StringWriter sw = new StringWriter();
                newException.printStackTrace(new PrintWriter(sw));
                stackTrace.setText(sw.toString());
            }
        });
    }

    private void setListener(){

    }
}