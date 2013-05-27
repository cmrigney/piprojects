package com.rigney.raspberrypiprojects;

import java.util.List;
import java.util.concurrent.Callable;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);  
        setContentView(R.layout.fragment_layout);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch(item.getItemId())
    	{
    	case R.id.menu_newpost:
    	{
    		Intent i = new Intent(this, NewPostActivity.class);
    		startActivity(i);
    		return true;
    	}
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    public static class DetailsActivity extends Activity
    {
    	@Override
    	protected void onCreate(Bundle savedInstanceState)
    	{
    		super.onCreate(savedInstanceState);
    		
    		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
    		{
    			//if in landscape, show dialog inline
    			finish();
    			return;
    		}
    		if(savedInstanceState == null)
    		{
    			DetailsFragment details = new DetailsFragment();
    			details.setArguments(getIntent().getExtras());
    			getFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
    		}
    	}
    }
    
    public static class TitlesFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<PiProject>>
    {
    	boolean mDualPane;
    	int mCurCheckPosition = -1;
    	
    	PiProjectAdapter mAdapter;
    	
    	public static boolean dialogShowing = false;
    	
    	Handler handler;
    	
    	@Override
    	public void onCreate(Bundle savedInstanceState)
    	{
    		super.onCreate(savedInstanceState);
    		setHasOptionsMenu(true);
    		
    	}
    	
    	@Override
    	public void onActivityCreated(Bundle savedInstanceState)
    	{
    		super.onActivityCreated(savedInstanceState);
    		
    		handler = new Handler();
    		
    		mAdapter = new PiProjectAdapter(getActivity(), new PiProject[]{});
    		
    		setListAdapter(mAdapter);
    		
    		if(!dialogShowing && getActivity().getSharedPreferences(AboutDialogFragment.PrefsName, 0).getBoolean(AboutDialogFragment.DialogShownSetting, false) == false)
    		{
    			AboutDialogFragment dialog = new AboutDialogFragment();
    			dialog.show(getFragmentManager(), null);
    			dialogShowing = true;
    		}
    		
    		View detailsFrame = getActivity().findViewById(R.id.details);
    		mDualPane = (detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE);
    		
    		if(savedInstanceState != null)
    		{
    			//restore last state for checked position
    			mCurCheckPosition = savedInstanceState.getInt("curChoice", -1);
    		}
    		
    		if(mDualPane)
    		{
    			//in dual pane, item gets highlighted
    			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    			showDetails(mCurCheckPosition);
    		}
    		
    		
    		getActivity().setProgressBarIndeterminateVisibility(true); 
    		getLoaderManager().initLoader(0, null, this);
    	}
    	
    	@Override
    	public void onSaveInstanceState(Bundle outState)
    	{
    		super.onSaveInstanceState(outState);
    		outState.putInt("curChoice", mCurCheckPosition);
    	}
    	
    	@Override
    	public void onListItemClick(ListView l, View v, int position, long id)
    	{
    		mAdapter.SetSelectedPos(position);
    		showDetails(position);
    	}
    	
    	void showDetails(int index)
    	{
    		if(index == -1) return;
    		
    		mCurCheckPosition = index;
    		
    		PiProject proj = null;
			if(mAdapter.GetProjects().size() > 0 && index >= 0 && index < mAdapter.GetProjects().size())
			{
				proj = mAdapter.GetProjects().get(index);
			}
    		
    		if(mDualPane)
    		{
    			getListView().setItemChecked(index, true);
    			
    			//check fragment shown, replace if needed
    			DetailsFragment details = (DetailsFragment)getFragmentManager().findFragmentById(R.id.details);
    			
    			if(details == null || details.getShownIndex() != index)
    			{
    				
    				details = DetailsFragment.newInstance(index, proj);
    				
    				//replace any existing fragment
    				FragmentTransaction ft = getFragmentManager().beginTransaction();
    				//if(index == 0)
    				//{
    					ft.replace(R.id.details, details);
    				//}
    				//else
    				//{
    					//ft.replace(R.id.menu_settings, details);
    				//}
    				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    				ft.commit();
    			}
    		}
    		else
    		{
    			//otherwise lauch new activity to show detail
    			Intent intent = new Intent();
    			intent.setClass(getActivity(), DetailsActivity.class);
    			intent.putExtra("index", index);
    			try {
					intent.putExtra("proj", proj.Serialize());
				} catch (Exception e) {
					e.printStackTrace();
				}
    			
    			startActivity(intent);
    		}
    	}
    	
    	void OnError(String error)
    	{
    		Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
    	}
    	
    	boolean CheckInternet()
    	{
    		ConnectivityManager connMgr = (ConnectivityManager) 
    	            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
    	    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    	    if (networkInfo != null && networkInfo.isConnected()) {
    	            return true;
    	    } else {
    	    		OnError("You are not connected to the internet.");
    	            return false;
    	    }
    	}

		@Override
		public Loader<List<PiProject>> onCreateLoader(int id, Bundle args) {
			return new PiProjectLoader(getActivity(), new Action<String>() {
				
				@Override
				public void Call(String value) {
					final String error = value;
					handler.post(new Runnable() {
						
						@Override
						public void run() {
							OnError(error);
						}
					});
				}
			}, CheckInternet());
		}

		@Override
		public void onLoadFinished(Loader<List<PiProject>> loader,
				List<PiProject> data) {
			mAdapter.AddProjects(data.toArray());
			mAdapter.notifyDataSetChanged();
			getActivity().setProgressBarIndeterminateVisibility(false); 
		}

		@Override
		public void onLoaderReset(Loader<List<PiProject>> loader) {
			if(loader instanceof PiProjectLoader)
			{
				((PiProjectLoader)loader).SetConnected(CheckInternet());
			}
			mAdapter.ClearProjects();
			mAdapter.notifyDataSetChanged();
		}
		
		@Override
	    public boolean onOptionsItemSelected(MenuItem item)
	    {
	    	switch(item.getItemId())
	    	{
	    	case R.id.menu_refresh:
	    	{
	    		getActivity().setProgressBarIndeterminateVisibility(true); 
	    		getLoaderManager().restartLoader(0, null, this);
	    		return true;
	    	}
	    	case R.id.menu_about:
	    	{
	    		AboutDialogFragment dialog = new AboutDialogFragment();
	    		dialog.show(getFragmentManager(), null);
	    	}
	    	}
	    	
	    	return super.onOptionsItemSelected(item);
	    }
		
    }
    
    public static class DetailsFragment extends Fragment
    {
    	
    	public static DetailsFragment newInstance(int index, PiProject project)
    	{
    		DetailsFragment f = new DetailsFragment();
    		
    		Bundle args = new Bundle();
    		args.putInt("index", index);
    		try {
				args.putString("proj", project.Serialize());
			} catch (Exception e) {
				e.printStackTrace();
			}
    		f.setArguments(args);
    		
    		return f;
    	}
    	
    	public int getShownIndex()
    	{
    		return getArguments().getInt("index", 0);
    	}
    	
    	public PiProject getProject()
    	{
    		String projStr = getArguments().getString("proj", null);
    		if(projStr == null) return null;
    		PiProject proj = new PiProject();
    		try {
				proj.Deserialize(projStr);
				return proj;
			} catch (Exception e) {
				e.printStackTrace();
			}
    		return null;
    	}
    	
    	
    	@Override
    	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    		if(container == null)
    		{
    			//will never be used
    			return null;
    		}
    		
    		ScrollView scroller = new ScrollView(getActivity());
    		
    		
    		PiProject project = getProject();
    		
    		if(project == null)
    		{
	    		return scroller; //return empty view
    		}
    		
    		LayoutInflater li = LayoutInflater.from(getActivity());
    		RelativeLayout ll = (RelativeLayout)li.inflate(R.layout.project_detail, null);
    		
    		((TextView)ll.findViewById(R.id.textViewURLDetail)).setText("(" + project.LinkUrl + ")");
    		((TextView)ll.findViewById(R.id.textViewDetailTitle)).setText(project.Title);
    		String author = project.Credit;
    		if(author == null || author.equalsIgnoreCase(""))
    		{
    			author = project.PostUser;
    		}
    		else
    		{
    			author += "\nSubmitted By: " + project.PostUser;
    		}
    		
    		
    		((TextView)ll.findViewById(R.id.textViewDetailAuthor)).setText("Author: " + author);
    		
    		((TextView)ll.findViewById(R.id.textViewDetailDescription)).setText("\t" + project.Description);
    		
    		ImageView img = (ImageView)ll.findViewById(R.id.imageViewDetailImage);
    		
    		if(project.PostImageUrl != null && !project.PostImageUrl.equalsIgnoreCase(""))
    		{
	    		PiProjectAdapter.FetchDecodeImageTask task = new PiProjectAdapter.FetchDecodeImageTask(img, 170.f, false);
	    		task.execute("", project.PostImageUrl);
    		}
    		else
    		{
    			img.setVisibility(View.GONE);
    		}
    		
    		Button viewButton = (Button)ll.findViewById(R.id.buttonViewLink);
    		if(project.LinkUrl == null || project.LinkUrl.equalsIgnoreCase(""))
    		{
    			viewButton.setVisibility(View.GONE);
    		}
    		else
    		{
	    		if (!project.LinkUrl.trim().startsWith("https://") && !project.LinkUrl.trim().startsWith("http://")){
	    			project.LinkUrl = "http://" + project.LinkUrl.trim();
	    		}
	    		final String url = project.LinkUrl;
	    		viewButton.setText("View Project");
	    		viewButton.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
						startActivity(i);
					}
				});
    		}
    		
    		scroller.addView(ll);
    		
    		return scroller;
    	}
    	
    }
}
