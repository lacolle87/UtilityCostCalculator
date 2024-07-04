package org.nj;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class UtilityCostCalculator extends JFrame {
    private static final String SETTINGS_FILE = "utility_settings.json";
    private JTextField initialWaterTextField, currentWaterTextField, initialElectricityTextField, currentElectricityTextField;
    private JTextField waterRateTextField, sewageRateTextField, electricityRateTextField;
    private JTextArea resultTextArea;

    public UtilityCostCalculator() {
        setTitle("Калькулятор коммунальных платежей");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ImageIcon icon = new ImageIcon("icon.png");
        setIconImage(icon.getImage());

        JPanel panel = createMainPanel();
        add(panel);

        loadSettings();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createInputPanel(), BorderLayout.CENTER);
        mainPanel.add(createResultArea(), BorderLayout.SOUTH);
        return mainPanel;
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);

        addLabelAndTextField(inputPanel, gbc, "Изначальные показатели воды:", initialWaterTextField = new JTextField(10));
        addLabelAndTextField(inputPanel, gbc, "Текущие показатели воды:", currentWaterTextField = new JTextField(10));
        addLabelAndTextField(inputPanel, gbc, "Изначальные показатели электричества:", initialElectricityTextField = new JTextField(10));
        addLabelAndTextField(inputPanel, gbc, "Текущие показатели электричества:", currentElectricityTextField = new JTextField(10));
        addLabelAndTextField(inputPanel, gbc, "Тариф за воду (руб/м³):", waterRateTextField = new JTextField(10));
        addLabelAndTextField(inputPanel, gbc, "Тариф за водоотведение (руб/м³):", sewageRateTextField = new JTextField(10));
        addLabelAndTextField(inputPanel, gbc, "Тариф за электричество (руб/кВтч):", electricityRateTextField = new JTextField(10));

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 5, 5, 5);
        JButton calculateButton = new JButton("Рассчитать");
        calculateButton.addActionListener(this::calculateUsageAndCost);
        inputPanel.add(calculateButton, gbc);

        return inputPanel;
    }

    private void addLabelAndTextField(JPanel panel, GridBagConstraints gbc, String labelText, JTextField textField) {
        JLabel label = new JLabel(labelText);
        gbc.gridx = 0;
        panel.add(label, gbc);

        gbc.gridx = 1;
        panel.add(textField, gbc);

        gbc.gridy++;
    }

    private JScrollPane createResultArea() {
        resultTextArea = new JTextArea(6, 30);
        resultTextArea.setEditable(false);
        return new JScrollPane(resultTextArea);
    }

    private void calculateUsageAndCost(ActionEvent e) {
        try {
            double initialWater = parseInput(initialWaterTextField);
            double currentWater = parseInput(currentWaterTextField);
            double initialElectricity = parseInput(initialElectricityTextField);
            double currentElectricity = parseInput(currentElectricityTextField);
            double waterRate = parseInput(waterRateTextField);
            double sewageRate = parseInput(sewageRateTextField);
            double electricityRate = parseInput(electricityRateTextField);

            validateInputs(initialWater, currentWater, initialElectricity, currentElectricity, waterRate, sewageRate, electricityRate);

            double waterCost = calculateCost(initialWater, currentWater, waterRate + sewageRate);
            double electricityCost = calculateCost(initialElectricity, currentElectricity, electricityRate);
            double totalCost = waterCost + electricityCost;

            displayResult(currentWater - initialWater, waterCost, currentElectricity - initialElectricity, electricityCost, totalCost);

            saveSettings(currentWater, currentElectricity, waterRate, sewageRate, electricityRate);
        } catch (NumberFormatException ex) {
            showErrorDialog("Пожалуйста, введите числовые значения во все поля");
        } catch (IllegalArgumentException ex) {
            showErrorDialog(ex.getMessage());
        } catch (IOException ioEx) {
            showErrorDialog("Ошибка при сохранении настроек: " + ioEx.getMessage());
        }
    }

    private double parseInput(JTextField textField) throws NumberFormatException {
        return Double.parseDouble(textField.getText());
    }

    private void validateInputs(double initialWater, double currentWater, double initialElectricity, double currentElectricity,
                                double waterRate, double sewageRate, double electricityRate) {
        if (initialWater < 0 || currentWater < 0 || initialElectricity < 0 || currentElectricity < 0
                || waterRate < 0 || sewageRate < 0 || electricityRate < 0) {
            throw new IllegalArgumentException("Значения не могут быть отрицательными.");
        }
        if (currentWater < initialWater || currentElectricity < initialElectricity) {
            throw new IllegalArgumentException("Текущие показатели не могут быть меньше изначальных.");
        }
    }

    private double calculateCost(double initialReading, double currentReading, double rate) {
        return (currentReading - initialReading) * rate;
    }

    private void displayResult(double waterUsage, double waterCost, double electricityUsage, double electricityCost, double totalCost) {
        String resultText = String.format(
                "Расход воды: %.3f м³%nСтоимость воды: %.2f руб%nРасход электричества: %.2f кВтч%nСтоимость электричества: %.2f руб%nИтого к оплате: %.2f руб%n",
                waterUsage, waterCost, electricityUsage, electricityCost, totalCost);
        resultTextArea.setText(resultText);
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
    }

    private void loadSettings() {
        try (JsonReader reader = Json.createReader(Files.newInputStream(Paths.get(SETTINGS_FILE)))) {
            JsonObject jsonObject = reader.readObject();
            initialWaterTextField.setText(jsonObject.getJsonNumber("initialWater").toString());
            initialElectricityTextField.setText(jsonObject.getJsonNumber("initialElectricity").toString());
            waterRateTextField.setText(jsonObject.getJsonNumber("waterRate").toString());
            sewageRateTextField.setText(jsonObject.getJsonNumber("sewageRate").toString());
            electricityRateTextField.setText(jsonObject.getJsonNumber("electricityRate").toString());
        } catch (IOException ex) {
            // If file doesn't exist or an error occurs, default values will be used
        }
    }

    private void saveSettings(double currentWater, double currentElectricity, double waterRate, double sewageRate, double electricityRate) throws IOException {
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("initialWater", currentWater)
                .add("initialElectricity", currentElectricity)
                .add("waterRate", waterRate)
                .add("sewageRate", sewageRate)
                .add("electricityRate", electricityRate)
                .build();

        try (JsonWriter writer = Json.createWriter(Files.newOutputStream(Paths.get(SETTINGS_FILE)))) {
            writer.writeObject(jsonObject);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UtilityCostCalculator::new);
    }
}

