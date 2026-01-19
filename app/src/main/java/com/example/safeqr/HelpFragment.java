package com.example.safeqr;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HelpFragment extends Fragment {

    public HelpFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_help, container, false);

        TextView tvEmail = view.findViewById(R.id.tvSupportEmail);

        tvEmail.setOnClickListener(v -> {
            String email = tvEmail.getText().toString().trim();

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
            intent.putExtra(Intent.EXTRA_SUBJECT, "SafeQR Support");
            intent.putExtra(Intent.EXTRA_TEXT, "Hello SafeQR team,\n\nI need help with:\n");

            try {
                startActivity(Intent.createChooser(intent, "Send email"));
            } catch (Exception e) {
                Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
