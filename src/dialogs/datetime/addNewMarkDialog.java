package dialogs.datetime;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class addNewMarkDialog {
    private static DateTimeController controller;
    private Stage stage;
    private TimestampPickedInterface timestampPickedInterface;

    public addNewMarkDialog() {initialise();}

    public void showAndWait(){
        if (stage!=null) stage.showAndWait();

    }

    public void setInterface(TimestampPickedInterface intf){timestampPickedInterface = intf;}

    private void initialise(){
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
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.initStyle(StageStyle.UNIFIED);
            stage.setTitle("Добавление новой отметки");
            stage.setScene(new Scene(root, -1, -1));

            // Событие при открытии окна.
            stage.setOnShown(event2 -> {

            });

            // Событие при закрытии окна.
            stage.setOnCloseRequest(event22 -> {
                stage.hide();
                if (timestampPickedInterface!=null && controller!=null){
                    if (controller.getTimestamp()!= null && !controller.getTimestamp().isEmpty())
                    timestampPickedInterface.picked(controller.getTimestamp(), controller.getComment());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        /////////////////////////////////////////////////////////////////////////
    }
}
