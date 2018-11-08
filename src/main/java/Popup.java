
import javax.swing.*;
import java.awt.*;

class Popup {

    private final String fileDir;

    private Popup(final String fileDir) {
        this.fileDir = fileDir;

    }

    static Popup prompt() {
        final JPanel mainPanel = new JPanel(new GridLayout(4, 1));

        mainPanel.add(new JLabel("Please enter the Absolute path of the .M file you would like to load: "));

        final JPanel panel = new JPanel();

        final JTextField numLines = new JTextField(5);
        panel.add(new JLabel("File Path:"));
        panel.add(numLines);


        mainPanel.add(panel);

        final int result = JOptionPane.showConfirmDialog(null, mainPanel,
                "Mesh Viewer", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            System.out.println("File Path:  " + numLines.getText());

        }
        return new Popup(numLines.getText());
    }

    String getFileDir() {
        return this.fileDir;
    }



}
