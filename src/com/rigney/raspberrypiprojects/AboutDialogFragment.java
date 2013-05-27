package com.rigney.raspberrypiprojects;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;

public class AboutDialogFragment extends DialogFragment {
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        ScrollView scroller = new ScrollView(getActivity());
        
        View v = inflater.inflate(R.layout.about_layout, null);
        
        ((Button)v.findViewById(R.id.buttonAboutDismiss)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AboutDialogFragment.this.dismiss();
			}
		});
        
        scroller.addView(v);
        
        builder.setView(scroller);
        
        
        return builder.create();
    }
	
	@Override
	public void onDismiss(DialogInterface dialog)
	{
		OnClose();
		super.onDismiss(dialog);
	}
	
	public static String PrefsName = "PiProjects";
	public static String DialogShownSetting = "DialogShownSettings";
	
	public void OnClose() {
		try
		{
		SharedPreferences pref = getActivity().getSharedPreferences(PrefsName, 0);
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean(DialogShownSetting, true);
		editor.commit();
		}
		catch(Exception ex){}
	}
	
}
