package com.rigney.raspberrypiprojects;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Vector;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PiProjectAdapter extends BaseAdapter {
	
	static class ViewHolder
	{
		AutoResizeTextView title;
		TextView creator;
		ImageView postUrl;
	}
	
	private LayoutInflater mInflater;
	
	private Vector<PiProject> piProjects;
	
	int mSelectedPos = -1;
	
	Drawable selected, notSelected;
	
	public PiProjectAdapter(Context context, PiProject[] projects)
	{
		mInflater = LayoutInflater.from(context);
		piProjects = new Vector<PiProject>();
		for(PiProject pi : projects)
		{
			piProjects.add(pi);
		}
		selected = context.getResources().getDrawable(R.drawable.projectbackgroundselected);
		notSelected = context.getResources().getDrawable(R.drawable.projectbackground);
	}
	
	public Vector<PiProject> GetProjects()
	{
		return piProjects;
	}
	
	public void ClearProjects()
	{
		piProjects.clear();
	}
	
	public void SetSelectedPos(int pos)
	{
		mSelectedPos = pos;
	}
	
	public void AddProjects(Object[] projects)
	{
		//handle ones that already exist inside
		Vector<PiProject> newProjs = new Vector<PiProject>();
		for(Object pi : projects)
		{
			PiProject proj = (PiProject)pi;
			boolean found = false;
			for(PiProject existProj : piProjects)
			{
				if(proj.ProjectId == existProj.ProjectId)
				{
					found = true;
					break;
				}
			}
			if(!found)
			{
				newProjs.add(proj);
			}
		}
		
		for(PiProject pi : piProjects)
		{
			newProjs.add(pi);
		}
		
		piProjects = newProjs;
	}
	
	public void AddProjects(PiProject[] projects)
	{
		//handle ones that already exist inside
		for(PiProject pi : projects)
		{
			piProjects.add(pi);
		}
	}

	public int getCount() {
		return piProjects.size();
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	
	
	@SuppressWarnings("deprecation")
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if(convertView == null)
		{
			convertView = mInflater.inflate(R.layout.project_listing, null);
			holder = new ViewHolder();
			holder.title = (AutoResizeTextView)convertView.findViewById(R.id.textViewTitle);
			holder.creator = (TextView)convertView.findViewById(R.id.textViewCredit);
			holder.postUrl = (ImageView)convertView.findViewById(R.id.imageFromUrl);
			convertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder)convertView.getTag();
		}
		
		
		holder.title.setSingleLine();
		holder.title.setMinTextSize(36);
		holder.title.setAddEllipsis(true);
		
		holder.title.setText(piProjects.get(position).Title);
		String credit = piProjects.get(position).Credit;
		if(credit == null || credit.equals(""))
			credit = piProjects.get(position).PostUser;
		
		holder.creator.setSingleLine();
		holder.title.setMinTextSize(26);
		holder.title.setAddEllipsis(true);
		
		holder.creator.setText("By " + credit);
		
		if(piProjects.get(position).PostImageUrl == null || piProjects.get(position).PostImageUrl.trim().equalsIgnoreCase(""))
		{
			//nothing is set for image
			holder.postUrl.setImageResource(R.drawable.device_access_storage);
			
		}
		else
		{
			String uid = "" + piProjects.get(position).ProjectId;
			
			Bitmap img = FetchDecodeImageTask.getBitmapFromMemCache(uid);
			if(img == null)
			{
				holder.postUrl.setImageBitmap(null);
				if(!WorkingImagesContains(uid))
				{
					AddWorkingImages(uid);
					FetchDecodeImageTask task = new FetchDecodeImageTask(holder.postUrl, 60.f, true);
					task.execute(uid, piProjects.get(position).PostImageUrl);
					
				}
			}
			else
			{
				holder.postUrl.setImageBitmap(img);
			}
		}
		
		if(mSelectedPos == position)
		{
			convertView.setBackgroundDrawable(selected);
		}
		else
		{
			convertView.setBackgroundDrawable(notSelected);
		}
		
		//convertView.setBackground(R.drawable.messagebackground);
		
		//convertView.setBackgroundColor(Color.CYAN);
		
		return convertView;
	}
	
	static LruCache<String, Bitmap> taskImageCache = null;
	
	static Vector<String> workingImages = new Vector<String>();
	public boolean WorkingImagesContains(String str)
	{
		for(String s : workingImages)
		{
			if(s.equalsIgnoreCase(str))
			{
				return true;
			}
		}
		return false;
	}
	public void AddWorkingImages(String str)
	{
		workingImages.add(str);
	}
	
	
	public static class FetchDecodeImageTask extends AsyncTask<String, Void, Bitmap>
	{
		WeakReference<ImageView> m_view;
		
		float m_size;
		boolean m_addToCache;
		
		public FetchDecodeImageTask(ImageView view, float size, boolean addToCache)
		{
			m_view = new WeakReference<ImageView>(view);
			m_size = size;
			m_addToCache = addToCache;
			
			if(taskImageCache == null && m_addToCache)
			{
				final int memClass = ((ActivityManager)view.getContext().getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
				
				final int cacheSize = 1024 * 1024 * memClass / 8;
				taskImageCache = new LruCache<String, Bitmap>(cacheSize) {
					@Override
					protected int sizeOf(String key, Bitmap bitmap)
					{
						return bitmap.getByteCount();
					}
				};
			}
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			
			
			Bitmap result = null;
			if(m_addToCache)
			{
				result = getBitmapFromMemCache(params[0]);
				if(result != null)
				{
					return result;
				}
			}
			
			try
			{
				InputStream is = (InputStream)new URL(params[1]).getContent();
				Bitmap bitmap = BitmapFactory.decodeStream(is);
				is.close();
				if(bitmap != null)
				{
					float ratio = (float)bitmap.getWidth() / (float)bitmap.getHeight();
					Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, (int)(m_size * ratio), (int)m_size, false);
					result = smallBitmap;
					if(result != null && m_addToCache)
					{
						addBitmapToMemoryCache(params[0], result);
					}
				}
			}
			catch(Exception ex){}
			
			return result;
		}
		
		@Override
		protected void onPostExecute(Bitmap result)
		{
			if(result == null) 
			{
				if(!m_addToCache) //may need changed in future
				{
					if(m_view != null)
					{
						if(m_view.get() != null)
						{
							m_view.get().setVisibility(View.GONE);
						}
					}
				}
				return;
			}
			
			if(m_view != null)
			{
				if(m_view.get() != null)
				{
					m_view.get().setImageBitmap(result);
				}
			}
		}
		
		static void addBitmapToMemoryCache(String key, Bitmap bitmap) 
		{
			if(getBitmapFromMemCache(key) == null && taskImageCache != null)
			{
				Log.d("PUT", "put");
				taskImageCache.put(key, bitmap);
			}
		}
		
		public static Bitmap getBitmapFromMemCache(String key)
		{
			if(taskImageCache == null) return null;
			return taskImageCache.get(key);
		}
		
	}
	
}