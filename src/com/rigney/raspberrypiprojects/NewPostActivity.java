package com.rigney.raspberrypiprojects;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class NewPostActivity extends Activity {
	
	EditText title;
	EditText author;
	EditText poster;
	EditText description;
	EditText projectUrl;
	EditText projectImageUrl;
	EditText captcha;
	Button submitButton;
	ImageView captchaImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ScrollView scroller = new ScrollView(this);
		
		LayoutInflater li = LayoutInflater.from(this);
		RelativeLayout ll = (RelativeLayout)li.inflate(R.layout.activity_new_post, null);
		
		title = (EditText)ll.findViewById(R.id.autoCompleteTextViewPostTitle);
		author = (EditText)ll.findViewById(R.id.editTextPostAuthorName);
		poster = (EditText)ll.findViewById(R.id.editTextPostYourName);
		description = (EditText)ll.findViewById(R.id.multiAutoCompleteTextViewPostDescription);
		projectUrl = (EditText)ll.findViewById(R.id.editTextPostProjectURL);
		projectImageUrl = (EditText)ll.findViewById(R.id.editTextPostProjectImageURL);
		captcha = (EditText)ll.findViewById(R.id.editTextPostCaptcha);
		captchaImage = (ImageView)ll.findViewById(R.id.imageCaptcha);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				FetchCaptcha();
			}
		}).start();
		
		submitButton = (Button)ll.findViewById(R.id.buttonPostSubmit);
		submitButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SubmitPost();
			}
		});
		
		scroller.addView(ll);
		
        setContentView(scroller);
    }
    
    volatile int captchaId = -1;
    
    void FetchCaptcha()
    {
    	
    	InputStream is = null;
	    BufferedInputStream buf = null;
    	
    	String getUrl = "http://piprojects.codyrigney.com/RequestCaptchaId.php";
    	try
    	{
	    	URL url = new URL(getUrl);
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setReadTimeout(10000 /* milliseconds */);
	        conn.setConnectTimeout(15000 /* milliseconds */);
	        conn.setRequestMethod("GET");
	        conn.setDoInput(true);
	        // Starts the query
	        conn.connect();
	        int response = conn.getResponseCode();
	        is = conn.getInputStream();
	
	        buf = new BufferedInputStream(is);
	        
	        byte[] buffer = ReadBytes(buf);
	        
	        String pageData = new String(buffer, "UTF-8");
	        if(pageData != null && pageData.trim().equalsIgnoreCase("Failed"))
	        {
	        	ShowErrorOnUi("Failed to get a captcha code. You won't be able to submit.");
	        	return;
	        }
	        
	        if(pageData == null || pageData.trim().equals(""))
	        {
	        	return; //silently quit
	        }
	        
	        if(buf != null)
	    	{
	    		try {
					buf.close();
				} catch (IOException e) {
				}
	    	}
	        if (is != null) {
	            try {
					is.close();
				} catch (IOException e) {
				}
	        } 
	        
	        captchaId = Integer.valueOf(pageData);
	        
	        getUrl = "http://piprojects.codyrigney.com/RequestCaptcha.php?Captcha=" + captchaId;
	        
	        url = new URL(getUrl);
	        conn = (HttpURLConnection) url.openConnection();
	        conn.setReadTimeout(10000 /* milliseconds */);
	        conn.setConnectTimeout(15000 /* milliseconds */);
	        conn.setRequestMethod("GET");
	        conn.setDoInput(true);
	        // Starts the query
	        conn.connect();
	        response = conn.getResponseCode();
	        is = conn.getInputStream();
	
	        buf = new BufferedInputStream(is);
	        
	        final Bitmap bm = BitmapFactory.decodeStream(buf);
	        if(bm == null)
	        {
	        	ShowErrorOnUi("Failed to get a captcha code. You won't be able to submit.");
	        	return;
	        }
	        
	        this.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					if(captchaImage != null)
					{
						captchaImage.setImageBitmap(bm);
					}
				}
			});
	        
    	}
    	catch(Exception ex)
	    {
    		ShowErrorOnUi("Failed to get a captcha code. You won't be able to submit. Exception thrown: " + ex.toString());
	    }
	    finally {
	    	this.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					if(progress != null && progress.isShowing())
					{
						progress.dismiss();
						progress = null;
					}
				}
			});
	    	
	    	if(buf != null)
	    	{
	    		try {
					buf.close();
				} catch (IOException e) {
				}
	    	}
	        if (is != null) {
	            try {
					is.close();
				} catch (IOException e) {
				}
	        } 
	    }
    }
    
    void ShowMessage(String msg)
    {
    	Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
    
    void ShowErrorOnUi(String msg)
    {
    	final String msg2 = msg;
    	this.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				ShowMessage(msg2);
			}
		});
    }
    
    void ShowSucceded()
    {
    	this.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				ShowMessage("Your post has been submitted.");
				finish();
			}
		});
    }
    
    boolean VerifyFields()
    {
    	if(title.getText().toString().trim().equals(""))
    	{
    		ShowMessage("Please enter a title.");
    		return false;
    	}
    	else if(poster.getText().toString().trim().equals(""))
    	{
    		ShowMessage("Please enter your name.");
    		return false;
    	}
    	else if(description.getText().toString().trim().equals(""))
    	{
    		ShowMessage("Please enter a description.");
    		return false;
    	}
    	else if(captchaId == -1)
    	{
    		ShowMessage("Please wait for the captcha to load. If it takes too long, back out and try again.");
    		return false;
    	}
    	
    	
    	else if(title.getText().toString().length() >= 64)
    	{
    		ShowMessage("The title can't be longer than 64 characters.");
    		return false;
    	}
    	else if(poster.getText().toString().length() >= 64 || author.getText().toString().length() >= 64)
    	{
    		ShowMessage("The names you entered can't be longer than 64 characters.");
    		return false;
    	}
    	else if(description.getText().toString().length() >= 8192)
    	{
    		ShowMessage("Your description is too long! Leave the steps to the URL you submit.");
    		return false;
    	}
    	else if(projectUrl.getText().toString().length() >= 256 || projectImageUrl.getText().toString().length() >= 256)
    	{
    		ShowMessage("Your URLs can't be longer than 256 characters. Sorry.");
    		return false;
    	}
    	else if(captcha.getText().toString().trim().equals(""))
    	{
    		ShowMessage("Please enter the captcha to proceed.");
    		return false;
    	}
    	
    	return true;
    }
    
    ProgressDialog progress = null;
    
    void SubmitPost()
    {
    	if(!VerifyFields())
    		return;
    	
    	progress = ProgressDialog.show(this, "",  "Submitting Project", true);
    	progress.setCancelable(false);
    	
    	new Thread(new Runnable() {
			
			@Override
			public void run() {
				DoSubmitWork();
			}
		}).start();
    	
    }
    
    void DoSubmitWork()
    {
    	InputStream is = null;
	    BufferedInputStream buf = null;
    	
    	try
    	{
    		String getUrl = "http://piprojects.codyrigney.com/PostProject.php?";
        	getUrl += "title=" + URLEncoder.encode(title.getText().toString().trim(), "UTF-8");
        	getUrl += "&desc=" + URLEncoder.encode(description.getText().toString().trim(), "UTF-8");
        	getUrl += "&imgurl=" + URLEncoder.encode(projectImageUrl.getText().toString().trim(), "UTF-8");
        	getUrl += "&linkurl=" + URLEncoder.encode(projectUrl.getText().toString().trim(), "UTF-8");
        	getUrl += "&user=" + URLEncoder.encode(poster.getText().toString().trim(), "UTF-8");
        	getUrl += "&credit=" + URLEncoder.encode(author.getText().toString().trim(), "UTF-8");
        	getUrl += "&captchaId=" + URLEncoder.encode("" + captchaId, "UTF-8");
        	getUrl += "&captcha=" + URLEncoder.encode(captcha.getText().toString().trim(), "UTF-8");
        	
    		URL url = new URL(getUrl);
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setReadTimeout(10000 /* milliseconds */);
	        conn.setConnectTimeout(15000 /* milliseconds */);
	        conn.setRequestMethod("GET");
	        conn.setDoInput(true);
	        // Starts the query
	        conn.connect();
	        int response = conn.getResponseCode();
	        is = conn.getInputStream();

	        buf = new BufferedInputStream(is);
	        
	        byte[] buffer = ReadBytes(buf);
	        
	        String pageData = new String(buffer, "UTF-8");
	        if(pageData != null && pageData.trim().equalsIgnoreCase("Succeeded"))
	        {
	        	ShowSucceded();
	        }
	        else if(pageData != null && pageData.trim().equalsIgnoreCase("Failed Captcha"))
	        {
	        	ShowErrorOnUi("Captcha was wrong. Please try again.");
	        }
	        else
	        {
	        	ShowErrorOnUi("Failed to submit post. Please try again.");
	        }
    	}
    	catch(Exception ex)
	    {
    		ShowErrorOnUi("Failed to submit post. Please try again. Exception thrown: " + ex.toString());
	    }
	    finally {
	    	this.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					if(progress != null && progress.isShowing())
					{
						progress.dismiss();
						progress = null;
					}
				}
			});
	    	
	    	if(buf != null)
	    	{
	    		try {
					buf.close();
				} catch (IOException e) {
				}
	    	}
	        if (is != null) {
	            try {
					is.close();
				} catch (IOException e) {
				}
	        } 
	    }
    }
    
    byte[] ReadBytes(InputStream stream) throws IOException
	  {
		  int val = stream.read();
		  ByteArrayOutputStream out = new ByteArrayOutputStream();
		  while(val > 0)
		  {
			  out.write(val);
			  val = stream.read();
		  }
		  out.flush();
		  byte[] result = out.toByteArray();
		  out.close();
		  return result;
	  }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_new_post, menu);
        return true;
    }
    */
}
