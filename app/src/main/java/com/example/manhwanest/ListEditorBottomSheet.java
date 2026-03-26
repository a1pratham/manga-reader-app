package com.example.manhwanest;

import android.os.Bundle;
import android.text.InputFilter;
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

        // 🔥 LIMIT SCORE INPUT (max 2 digits)
        scoreInput.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(2)
        });

        // 🔥 DROPDOWN SETUP
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                statusOptions
        );

        statusDropdown.setAdapter(adapter);

        // Show dropdown on click
        statusDropdown.setOnClickListener(v -> statusDropdown.showDropDown());

        // Default value
        statusDropdown.setText("Planning", false);

        // 🔥 PREFILL VALUES (IMPORTANT)
        Bundle args = getArguments();

        if (args != null) {
            statusDropdown.setText(args.getString("status", "Planning"), false);
            progressInput.setText(String.valueOf(args.getInt("progress", 0)));

            int score = args.getInt("score", 0);
            if (score != 0) {
                scoreInput.setText(String.valueOf(score));
            }
        }

        // ➕ +1 BUTTON
        plusButton.setOnClickListener(v -> {
            String current = progressInput.getText().toString();
            int value = current.isEmpty() ? 0 : Integer.parseInt(current);
            value++;
            progressInput.setText(String.valueOf(value));
        });

        // 📅 START DATE
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

            String status = statusDropdown.getText().toString();
            String progress = progressInput.getText().toString().trim();
            String score = scoreInput.getText().toString().trim();

            // 🔥 SCORE VALIDATION (1–10)
            if (!TextUtils.isEmpty(score)) {
                int s = Integer.parseInt(score);
                if (s < 1 || s > 10) {
                    scoreInput.setError("Score must be 1-10");
                    return;
                }
            }

            // 🔥 SEND BACK
            if (listener != null) {
                listener.onSave(status, progress, score);
            }

            dismiss();
        });

        return view;
    }
}