/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2018 Nikita Shakarun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.lcdui.list;

import android.database.DataSetObserver;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;

public abstract class CompoundAdapter implements Adapter, Handler.Callback {

	private static final int NOTIFY_CHANGED = 1;
	private static final int NOTIFY_INVALIDATED = 2;

	// A lock object to synchronize access to the 'items' list.
	private final Object mLock = new Object();
	private final Handler mHandler = new Handler(Looper.getMainLooper(), this);
	private final ArrayList<CompoundItem> items = new ArrayList<>();
	private final ArrayList<DataSetObserver> observers = new ArrayList<>();

	public CompoundAdapter() {}

	public void add(String stringPart, Image imagePart) {
		synchronized (mLock) {
			items.add(new CompoundItem(stringPart, imagePart));
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	public void insert(int elementNum, String stringPart, Image imagePart) {
		synchronized (mLock) {
			items.add(elementNum, new CompoundItem(stringPart, imagePart));
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	public void set(int elementNum, String stringPart, Image imagePart) {
		synchronized (mLock) {
			items.set(elementNum, new CompoundItem(stringPart, imagePart));
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	public void delete(int elementNum) {
		synchronized (mLock) {
			items.remove(elementNum);
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	public void deleteAll() {
		synchronized (mLock) {
			items.clear();
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	public void setAll(List<CompoundItem> newItems) {
		synchronized (mLock) {
			items.clear();
			if (newItems != null) {
				items.addAll(newItems);
			}
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	@Override
	public int getCount() {
		synchronized (mLock) {
			return items.size();
		}
	}

	@Override
	public boolean isEmpty() {
		synchronized (mLock) {
			return items.isEmpty();
		}
	}

	@Override
	public CompoundItem getItem(int position) {
		synchronized (mLock) {
			return items.get(position);
		}
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	View getView(int position, View convertView, ViewGroup parent, int viewResourceID, boolean useImagePart) {
		TextView textview;

		if (convertView instanceof TextView) {
			textview = (TextView) convertView;
		} else {
			textview = (TextView) LayoutInflater.from(parent.getContext()).inflate(viewResourceID, null);
		}

		CompoundItem item = getItem(position); // Uses the synchronized getItem()

		if (useImagePart && item.getImage() != null) {
			Paint.FontMetrics fm = textview.getPaint().getFontMetrics();
			float lineHeight = fm.leading + fm.bottom - fm.top;
			Drawable drawable = item.getDrawable(lineHeight);
			SpannableStringBuilder ssb = new SpannableStringBuilder(" ");
			ImageSpan imageSpan = new ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BOTTOM);
			ssb.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.append(item.getString());
			textview.setText(ssb);
		} else {
			textview.setText(item.getString());
		}

		return textview;
	}

	@Override
	public abstract View getView(int position, View convertView, ViewGroup parent);

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		synchronized (mLock) {
			if (!observers.contains(observer)) {
				observers.add(observer);
			}
		}
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		synchronized (mLock) {
			observers.remove(observer);
		}
	}

	public void add(CompoundItem item) {
		synchronized (mLock) {
			items.add(item);
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	public void insert(int index, CompoundItem item, boolean clearSelection) {
		synchronized (mLock) {
			if (clearSelection) {
				for (CompoundItem currentItem : items) {
					currentItem.setSelected(false);
				}
			}
			items.add(index, item);
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	public void setSelectionFlags(boolean[] selectedArray) {
		synchronized (mLock) {
			for (int i = 0; i < selectedArray.length && i < items.size(); i++) {
				items.get(i).setSelected(selectedArray[i]);
			}
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	public void setSelection(int index, boolean flag) {
		synchronized (mLock) {
			items.get(index).setSelected(flag);
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	public void setExclusiveSelection(int index) {
		synchronized (mLock) {
			for (int i = 0; i < items.size(); i++) {
				items.get(i).setSelected(i == index);
			}
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	public void setFont(int index, Font font) {
		synchronized (mLock) {
			items.get(index).setFont(font);
		}
		mHandler.sendEmptyMessage(NOTIFY_CHANGED);
	}

	@Override
	public boolean handleMessage(Message msg) {
		// Create a copy of the observers list to avoid ConcurrentModificationException
		// if an observer unregisters itself inside its onChanged/onInvalidated method.
		ArrayList<DataSetObserver> observersCopy;
		synchronized (mLock) {
			observersCopy = new ArrayList<>(observers);
		}

		switch (msg.what) {
			case NOTIFY_CHANGED:
				for (DataSetObserver observer : observersCopy) {
					try {
						observer.onChanged();
					} catch (Exception e) {
						// Log or handle the exception
						e.printStackTrace();
					}
				}
				return true;
			case NOTIFY_INVALIDATED:
				for (DataSetObserver observer : observersCopy) {
					observer.onInvalidated();
				}
				return true;
		}
		return false;
	}
}