package dialogs.time;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class TimeDialog {
    private static TimeController controller;
    private Stage stage;
    private TimePickedInterface timePickedInterface;

    public TimeDialog(Window owner, Node node, String init_time) {

        Double x = null;
        Double y = null;

        // Вычисляем позицию окна на экране.
        if (node != null) {

            Bounds boundsInScreen = node.localToScreen(node.getBoundsInLocal());

            x = boundsInScreen.getMinX();
            y = boundsInScreen.getMaxY();
        }

        initialise(owner, x, y, init_time);
    }

    public void showAndWait(){
        if (stage!=null) stage.showAndWait();
    }

    public void setInterface(TimePickedInterface callback){
        timePickedInterface = callback;}

    private void initialise(Window owner, Double x, Double y, String init_time){

        ///////////////////////////////
        // Запускаем диалоговое окно //
        ///////////////////////////////

        FXMLLoader loader = new FXMLLoader(getClass().getResource("dialog.fxml"));
        try {

            Parent root = loader.load();
            controller = loader.getController();
            //SettingsController settingsController = loader.getController();
            stage = new Stage();
            stage.getIcons().add(new Image("/images/date_and_time.png"));
            // Установить это диалоговое окно поверх остальных окон и ждать его закрытия.
            //stage.initModality(Modality.APPLICATION_MODAL);
            //stage.initModality(Modality.WINDOW_MODAL);
            stage.initModality(Modality.NONE);

            stage.setResizable(false);
            //stage.initStyle(StageStyle.UNIFIED);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Выбор времени");
            stage.setScene(new Scene(root, -1, -1));

            if (x != null) stage.setX(x);
            if (y != null) stage.setY(y);

            stage.initOwner(owner);

            // Событие при открытии окна.
            stage.setOnShown(event2 -> {
                // Событие при изменении фокуса окна.
                stage.focusedProperty().addListener((ov, oldValue, newValue) -> {
                    if (!newValue){
                        stage.hide();
                    }
                });
            });

            // Событие при закрытии окна.
            stage.setOnCloseRequest(event22 -> {
                stage.hide();
                if (timePickedInterface !=null && controller!=null){
                    if (controller.getTimestamp()!= null && !controller.getTimestamp().isEmpty())
                    timePickedInterface.picked(controller.getTimestamp());
                }
            });

            controller.init_and_show(init_time);

        } catch (Exception e) {
            e.printStackTrace();
        }
        /////////////////////////////////////////////////////////////////////////
    }
}
