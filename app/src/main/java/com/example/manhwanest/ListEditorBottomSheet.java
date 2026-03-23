package com.example.manhwanest;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ListEditorBottomSheet extends BottomSheetDialogFragment {

    AutoCompleteTextView statusDropdown;
    EditText progressInput, scoreInput;
    Button saveButton, plusButton, startDateButton, endDateButton, deleteButton;

    String[] statusOptions = {
            "Planning", "Reading", "Completed", "Re-reading", "Paused", "Dropped"
    };

    // 🔥 INTERFACE
    public interface OnSaveListener {
        void onSave(String status, String progress, String score);
    }

    OnSaveListener listener;

    public void setOnSaveListener(OnSaveListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_sheet_list_editor, container, false);

        // 🔥 Bind views
        statusDropdown = view.findViewById(R.id.statusDropdown);
        progressInput = view.findViewById(R.id.progressInput);
        scoreInput = view.findViewById(R.id.scoreInput);
        saveButton = view.findViewById(R.id.saveButton);
        plusButton = view.findViewById(R.id.plusButton);
        startDateButton = view.findViewById(R.id.startDateButton);
        endDateButton = view.findViewById(R.id.endDateButton);
        deleteButton = view.findViewById(R.id.deleteButton);

        // 🔥 DROPDOWN SETUP (FIXED)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                statusOptions
        );

        statusDropdown.setAdapter(adapter);

        // 🔥 Show dropdown on click (IMPORTANT)
        statusDropdown.setOnClickListener(v -> statusDropdown.showDropDown());

        // Default value
        statusDropdown.setText("Planning", false);

        // ➕ +1 BUTTON
        plusButton.setOnClickListener(v -> {
            String current = progressInput.getText().toString();
            int value = current.isEmpty() ? 0 : Integer.parseInt(current);
            value++;
            progressInput.setText(String.valueOf(value));
        });

        // 📅 START DATE (simple placeholder)
        startDateButton.setOnClickListener(v -> {
            startDateButton.setText("Today");
        });

        // 📅 END DATE
        endDateButton.setOnClickListener(v -> {
            endDateButton.setText("Completed");
        });

        // 🗑 DELETE BUTTON
        deleteButton.setOnClickListener(v -> {
            progressInput.setText("");
            scoreInput.setText("");
            statusDropdown.setText("Planning", false);
        });

        // 💾 SAVE BUTTON
        saveButton.setOnClickListener(v -> {

            String status = statusDropdown.getText().toString(); // ✅ FIXED
            String progress = progressInput.getText().toString().trim();
            String score = scoreInput.getText().toString().trim();

            // 🔥 SCORE VALIDATION
            if (!TextUtils.isEmpty(score)) {
                int s = Integer.parseInt(score);
                if (s < 1 || s > 10) {
                    scoreInput.setError("Score must be 1-10");
                    return;
                }
            }

            // 🔥 Send back
            if (listener != null) {
                listener.onSave(status, progress, score);
            }

            dismiss();
        });

        return view;
    }
}