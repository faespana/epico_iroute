package com.iroute.epico.apirest_files.controllers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.iroute.epico.apirest_files.Utils.ConnectionDB;
import com.opencsv.CSVReader;

@RestController
@RequestMapping("/file")
public class fileController {

    @PostMapping("/uploadfile")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo está vacío");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        CSVReader csvParser = new CSVReader(reader);
        Connection conn = ConnectionDB.createConnection()){

            CallableStatement statement = conn.prepareCall("{call sp_create_commerce(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "+
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");

            String[] headers = csvParser.readNext(); // Leer las cabeceras

            String[] values;
            while((values = csvParser.readNext()) != null){
                int fieldCount = 1;
                for (String valuesRow : values) {
                    statement.setString(fieldCount, valuesRow);
                    fieldCount++;
                }
                statement.addBatch();
            }

            statement.executeBatch();
            conn.close();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al procesar el archivo: " + e.getMessage());
        }
        return ResponseEntity.ok().body("Datos insertados exitosamente");
    }
    @PostMapping("/validate")
    public ResponseEntity<?> validateRecords(@RequestParam("process_date") String processDate) {
        try (Connection conn = ConnectionDB.createConnection()) {
            CallableStatement statement = conn.prepareCall("{call sp_validate_commerce(?)}");
            statement.setString(1, processDate);
            boolean hasResults = statement.execute();

            int recordsMoved = 0;
            if (hasResults) {
                try (ResultSet rs = statement.getResultSet()) {
                    if (rs.next()) {
                        recordsMoved = rs.getInt("records_moved");
                    }
                }
            }

            conn.close();
            return ResponseEntity.ok().body("Registros movidos a commerce_quarantine: " + recordsMoved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al validar los registros: " + e.getMessage());
        }
    }
    @GetMapping("/quarantine")
    public ResponseEntity<?> getQuarantineRecords() {
        try (Connection conn = ConnectionDB.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM commerce_quarantine")) {

            List<Map<String, Object>> records = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("pc_id", rs.getInt("pc_id"));
                record.put("pc_codent", rs.getString("pc_codent"));
                record.put("pc_centalta", rs.getString("pc_centalta"));
                // Agrega el resto de las columnas según sea necesario
                records.add(record);
            }

            return ResponseEntity.ok().body(records);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener los registros: " + e.getMessage());
        }
    }

}
