package com.rigney.raspberrypiprojects;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.CharBuffer;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.util.ByteArrayBuffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class PiProjectLoader extends AsyncTaskLoader<List<PiProject>> {
	 
	  // We hold a reference to the Loader’s data here.
	  private List<PiProject> mData;
	  
	  int m_lastProjectId = 0;
	  Action<String> m_errorCallback;
	  boolean m_connected;
	 
	  public PiProjectLoader(Context ctx, Action<String> errorCallback, boolean connected) {
	    // Loaders may be used across multiple Activitys (assuming they aren't
	    // bound to the LoaderManager), so NEVER hold a reference to the context
	    // directly. Doing so will cause you to leak an entire Activity's context.
	    // The superclass constructor will store a reference to the Application
	    // Context instead, and can be retrieved with a call to getContext().
	    super(ctx);
	    m_errorCallback = errorCallback;
	    m_connected = connected;
	  }
	  
	  public void SetConnected(boolean isCon)
	  {
		  m_connected = isCon;
	  }
	 
	  /****************************************************/
	  /** (1) A task that performs the asynchronous load **/
	  /****************************************************/
	 
	  @Override
	  public List<PiProject> loadInBackground() {
	    // This method is called on a background thread and should generate a
	    // new set of data to be delivered back to the client.
	    List<PiProject> data = new ArrayList<PiProject>();
	    
	    if(!m_connected) return data;
	    
	    InputStream is = null;
	    BufferedInputStream buf = null;
	    
	    String getUrl = "http://ec2-54-69-196-185.us-west-2.compute.amazonaws.com/piprojects/GetPostDataXml.php?LastPost=" + m_lastProjectId;
	        
	    String debugPage = "";
	    
	    try {
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
	        debugPage = pageData;
	        
	        if(pageData == null || pageData.trim().equals(""))
	        {
	        	return data;
	        }
	        
	        int idx = pageData.indexOf("<?xml");
	        if(idx == -1)
	        {
	        	m_errorCallback.Call("Failed to fetch the needed information.  Please notify the administrator.");
	        	return data;
	        }
	        int len = pageData.length();
	        pageData = pageData.substring(idx, len);
	        
	        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        DocumentBuilder db = dbf.newDocumentBuilder();
	        ByteArrayInputStream bin = new ByteArrayInputStream(pageData.getBytes("UTF-8"));
	        Document doc = db.parse(bin);
	        
	        if(doc == null) 
	        {
	        	m_errorCallback.Call("Could not parse XML. Invalid data.");
	        	return data;
	        }
	        
	        bin.close();
	        
	        Element header = doc.getDocumentElement();
	        
	        NodeList nodes = header.getChildNodes();
	        if(nodes.getLength() == 0) Log.d("NOTNEEDED", "No nodes fetched");
	        
	        for(int i1 = 0; i1 < nodes.getLength(); i1++)
	        {
	        	Node n = nodes.item(i1);
	        	Element e = (Element)n;
	        	//a pi project object at this point
	        	PiProject piProj = new PiProject();
	        	piProj.DeserializeFromElement(e);
	        	data.add(piProj);
	        }
	        
	        for(PiProject pi : data)
	        {
	        	if(pi.ProjectId > m_lastProjectId)
	        	{
	        		m_lastProjectId = pi.ProjectId;
	        	}
	        }
	        
	    } 
	    catch(Exception ex)
	    {
	    	m_errorCallback.Call("Failed to fetch data.  Exception thrown: " + ex.toString() + " - Page Data: " + debugPage);
	    }
	    finally {
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
	    
	 
	    return data;
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
	 
	  /********************************************************/
	  /** (2) Deliver the results to the registered listener **/
	  /********************************************************/
	 
	  @Override
	  public void deliverResult(List<PiProject> data) {
	    if (isReset()) {
	      // The Loader has been reset; ignore the result and invalidate the data.
	      onReleaseResources(data);
	      return;
	    }
	 
	    // Hold a reference to the old data so it doesn't get garbage collected.
	    // The old data may still be in use (i.e. bound to an adapter, etc.), so
	    // we must protect it until the new data has been delivered.
	    List<PiProject> oldData = mData;
	    mData = data;
	 
	    if (isStarted()) {
	      // If the Loader is in a started state, deliver the results to the
	      // client. The superclass method does this for us.
	      super.deliverResult(data);
	    }
	 
	    // Invalidate the old data as we don't need it any more.
	    if (oldData != null && oldData != data) {
	      onReleaseResources(oldData);
	    }
	  }
	 
	  /*********************************************************/
	  /** (3) Implement the Loader’s state-dependent behavior **/
	  /*********************************************************/
	 
	  @Override
	  protected void onStartLoading() {
	    if (mData != null) {
	      // Deliver any previously loaded data immediately.
	      deliverResult(mData);
	    }
	 
	    // Begin monitoring the underlying data source.
	    //if (mObserver == null) {
	      //mObserver = new SampleObserver();
	      // TODO: register the observer
	    //}
	 
	    if (takeContentChanged() || mData == null) {
	      // When the observer detects a change, it should call onContentChanged()
	      // on the Loader, which will cause the next call to takeContentChanged()
	      // to return true. If this is ever the case (or if the current data is
	      // null), we force a new load.
	      forceLoad();
	    }
	  }
	 
	  @Override
	  protected void onStopLoading() {
	    // The Loader is in a stopped state, so we should attempt to cancel the 
	    // current load (if there is one).
	    cancelLoad();
	 
	    // Note that we leave the observer as is; Loaders in a stopped state
	    // should still monitor the data source for changes so that the Loader
	    // will know to force a new load if it is ever started again.
	  }
	 
	  @Override
	  protected void onReset() {
	    // Ensure the loader has been stopped.
	    onStopLoading();
	 
	    // At this point we can release the resources associated with 'mData'.
	    if (mData != null) {
	      onReleaseResources(mData);
	      mData = null;
	    }
	 
	    // The Loader is being reset, so we should stop monitoring for changes.
	    //if (mObserver != null) {
	      // TODO: unregister the observer
	     // mObserver = null;
	    //}
	  }
	 
	  @Override
	  public void onCanceled(List<PiProject> data) {
	    // Attempt to cancel the current asynchronous load.
	    super.onCanceled(data);
	 
	    // The load has been canceled, so we should release the resources
	    // associated with 'data'.
	    onReleaseResources(data);
	  }
	 
	  protected void onReleaseResources(List<PiProject> data) {
	    // For a simple List, there is nothing to do. For something like a Cursor, we 
	    // would close it in this method. All resources associated with the Loader
	    // should be released here.
	  }
	 
	  /*********************************************************************/
	  /** (4) Observer which receives notifications when the data changes **/
	  /*********************************************************************/
	  
	  // NOTE: Implementing an observer is outside the scope of this post (this example
	  // uses a made-up “SampleObserver” to illustrate when/where the observer should 
	  // be initialized). 
	   
	  // The observer could be anything so long as it is able to detect content changes
	  // and report them to the loader with a call to onContentChanged(). For example,
	  // if you were writing a Loader which loads a list of all installed applications
	  // on the device, the observer could be a BroadcastReceiver that listens for the
	  // ACTION_PACKAGE_ADDED intent, and calls onContentChanged() on the particular 
	  // Loader whenever the receiver detects that a new application has been installed.
	  // Please don’t hesitate to leave a comment if you still find this confusing! :)
	  //private SampleObserver mObserver;
}
