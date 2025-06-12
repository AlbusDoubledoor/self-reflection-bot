package google.api;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class SheetService {
    private static final String APPLICATION_NAME = "Self-Reflection Bot";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String GOOGLE_API_SERVICE_ACCOUNT = System.getenv("google_api_service_account");
    private static final String TARGET_SPREADSHEET_ID = System.getenv("google_api_spreadsheet_id");
    private static final String TARGET_SPREADSHEET_NAME = System.getenv("google_api_sheet_name");

    public static Sheets getSheetsService() throws NullPointerException, IOException, GeneralSecurityException {
        if (GOOGLE_API_SERVICE_ACCOUNT == null) {
            throw new NullPointerException("Google API service account not found");
        }

        InputStream in = new FileInputStream(GOOGLE_API_SERVICE_ACCOUNT);

        GoogleCredentials credentials = ServiceAccountCredentials
                .fromStream(in)
                .createScoped(SCOPES);

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static int getRowIdxForEntry(String columnRange, String targetEntry) throws NullPointerException, IOException {
        Sheets sheetService;
        try {
            sheetService = getSheetsService();
        } catch (Exception ex) {
            throw new NullPointerException("Sheets service not acquired. " + ex.getMessage());
        }

        String spreadsheetId = getSpreadsheetId();
        String dateColumnRange = getSheetName() + "!" + columnRange;

        ValueRange response = sheetService.spreadsheets().values()
                .get(spreadsheetId, dateColumnRange)
                .execute();

        List<List<Object>> values = response.getValues();

        if (values == null) {
            return -1;
        }

        for (int rowIndex = 0; rowIndex < values.size(); ++rowIndex) {
            List<Object> row = values.get(rowIndex);
            if (!row.isEmpty()) {
                String cellText = row.getFirst().toString().trim();
                if (cellText.equals(targetEntry)) {
                    return rowIndex;
                }
            }
        }

        return -1;
    }

    public static int getColumnIdxForRowEntry(int rowIdx, String targetEntry) throws NullPointerException, IOException {
        Sheets sheetService;
        try {
            sheetService = getSheetsService();
        } catch (Exception ex) {
            throw new NullPointerException("Sheets service not acquired. " + ex.getMessage());
        }

        String spreadsheetId = getSpreadsheetId();
        String dateColumnRange = getSheetName() + "!" + (rowIdx + 1) + ":" + (rowIdx + 1);

        ValueRange response = sheetService.spreadsheets().values()
                .get(spreadsheetId, dateColumnRange)
                .execute();

        List<List<Object>> values = response.getValues();

        if (values == null) {
            return -1;
        }

        if (!values.isEmpty()) {
            List<Object> row = values.getFirst();
            for (int columnIdx = 0; columnIdx < row.size(); ++columnIdx) {
                if (row.get(columnIdx).toString().trim().equals(targetEntry)) {
                    return columnIdx;
                }
            }
        }

        return -1;
    }

    public static void updateCellValue(int rowIdx, int colIdx, String value) throws IOException {
        Sheets sheetService;
        try {
            sheetService = getSheetsService();
        } catch (Exception ex) {
            throw new NullPointerException("Sheets service not acquired. " + ex.getMessage());
        }

        ValueRange body = new ValueRange()
                .setValues(List.of(Collections.singletonList(value)));  // 2D list: one row, one column

        String range = getSheetName() + "!" + columnIndexToLetter(colIdx) + (rowIdx + 1);

        sheetService.spreadsheets().values()
                .update(getSpreadsheetId(), range, body)
                .setValueInputOption("RAW")
                .execute();
    }

    public static int appendRow(List<Object> rowValues) throws NullPointerException, IOException {
        Sheets sheetService;
        try {
            sheetService = getSheetsService();
        } catch (Exception ex) {
            throw new NullPointerException("Sheets service not acquired. " + ex.getMessage());
        }

        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowValues));

        AppendValuesResponse appendValuesResponse =
                sheetService.spreadsheets().values()
                                .append(getSpreadsheetId(), getSheetName(), body)
                                .setValueInputOption("RAW")
                                .setInsertDataOption("INSERT_ROWS")
                                .execute();

        String updatedRange = appendValuesResponse.getUpdates().getUpdatedRange();
        return extractRowIndexFromRange(updatedRange) - 1;
    }

    public static int extractRowIndexFromRange(String a1Range) {
        // Example input: "Sheet1!A10:A10"
        String[] parts = a1Range.split("!");
        if (parts.length < 2) return -1;

        String rangePart = parts[1]; // "A10:A10"
        String[] cellParts = rangePart.split(":");

        if (cellParts.length == 0) return -1;

        // Use last cell reference
        String lastCell = cellParts[cellParts.length - 1];

        // Extract digits from cell like "A10"
        String rowStr = lastCell.replaceAll("[^0-9]", "");
        return rowStr.isEmpty() ? -1 : Integer.parseInt(rowStr);
    }

    public static String columnIndexToLetter(int colIndex) {
        StringBuilder sb = new StringBuilder();
        colIndex++; // Convert from 0-based to 1-based
        while (colIndex > 0) {
            int rem = (colIndex - 1) % 26;
            sb.insert(0, (char) (rem + 'A'));
            colIndex = (colIndex - 1) / 26;
        }
        return sb.toString();
    }

    public static String getSpreadsheetId() {
        return TARGET_SPREADSHEET_ID;
    }

    public static String getSheetName() {
        return TARGET_SPREADSHEET_NAME;
    }
}
