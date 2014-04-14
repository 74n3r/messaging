/**
 * OpenERP, Open Source Management Solution
 * Copyright (C) 2012-today OpenERP SA (<http://www.openerp.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * 
 */
package com.openerp.addons.note;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.openerp.R;
import com.openerp.addons.message.MessageComposeActivity;
import com.openerp.addons.note.Note.NoteToggleStatus;
import com.openerp.orm.OEDataRow;
import com.openerp.orm.OEHelper;
import com.openerp.support.BaseFragment;
import com.openerp.support.fragment.FragmentListener;
import com.openerp.util.HTMLHelper;
import com.openerp.util.TextViewTags;
import com.openerp.util.drawer.DrawerItem;

public class NoteDetail extends BaseFragment {

	public static final String TAG = "com.openerp.addons.note.NoteDetail";
	View mView = null;
	Bundle mArgument = null;
	TextView mNoteDetailTitle, mNoteDetailMemo, mNoteTags = null;
	int mStageColor = Color.parseColor("#414141");
	String mPadURL = "";
	String mNoteMemo = "";
	String mMessageBody = "";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		mView = inflater.inflate(R.layout.fragment_note_detail, container,
				false);
		return mView;
	}

	@Override
	public void onStart() {
		super.onStart();
		Bundle bundle = getArguments();
		if (bundle != null) {
			mArgument = bundle;
			if (mArgument.containsKey("stage_color")) {
				View vStageColor = (View) mView
						.findViewById(R.id.viewNoteStageColor);
				vStageColor.setBackgroundColor(mArgument.getInt("stage_color"));
				mStageColor = mArgument.getInt("stage_color");
			}
		}
	}

	private void showNoteDetails(int note_id) {
		mNoteDetailTitle = (TextView) mView
				.findViewById(R.id.txvNoteDetailTitle);
		mNoteDetailMemo = (TextView) mView.findViewById(R.id.txvNoteDetailMemo);
		mNoteTags = (TextView) mView.findViewById(R.id.edtNoteTagsView);

		mNoteDetailMemo.setMovementMethod(new ScrollingMovementMethod());
		OEDataRow result = db().select(note_id);
		if (!result.getString("note_pad_url").equals("false")) {
			mPadURL = result.getString("note_pad_url");
		}
		mNoteMemo = result.getString("memo");

		List<String> mNoteTagsItems = new ArrayList<String>();
		for (OEDataRow tag : result.getM2MRecord("tag_ids").browseEach()) {
			mNoteTagsItems.add(tag.getString("name"));
		}
		TextViewTags tags = new TextViewTags(getActivity(), mNoteTagsItems,
				mStageColor, "#ffffff", 25);
		mNoteTags.setText(tags.generate());

		mMessageBody = result.getString("memo");
		mNoteDetailTitle.setText(result.getString("name"));
		mNoteDetailMemo.setText(HTMLHelper.stringToHtml(result
				.getString("memo")));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		inflater.inflate(R.menu.menu_fragment_note_detail, menu);
		// disabling the Compose Note option cause you are already in that menu
		if (getArguments() != null) {
			mArgument = getArguments();
			boolean open = mArgument.getBoolean("row_status");
			if (open) {
				MenuItem mark_as_open = menu
						.findItem(R.id.menu_note_mark_asopen);
				mark_as_open.setVisible(false);
			} else {
				MenuItem mark_as_done = menu
						.findItem(R.id.menu_note_mark_asdone);
				mark_as_done.setVisible(false);
			}

		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		NoteToggleStatus status = null;
		Note note = new Note();
		switch (item.getItemId()) {
		case R.id.menu_note_followers:

			NoteFollowers noteFollowers = new NoteFollowers();
			Bundle args = new Bundle();
			args.putInt("note_id", mArgument.getInt("note_id"));
			args.putString("note_name", mNoteDetailTitle.getText().toString());
			noteFollowers.setArguments(args);
			FragmentListener mFragment = (FragmentListener) getActivity();
			mFragment.startDetailFragment(noteFollowers);
			return true;

		case R.id.menu_note_forward_asmail:

			Intent sendAsMail = new Intent(getActivity(),
					MessageComposeActivity.class);
			sendAsMail.putExtra("note_body", mMessageBody);
			getActivity().startActivity(sendAsMail);
			return true;

		case R.id.menu_note_mark_asdone:

			status = note.new NoteToggleStatus(mArgument.getInt("note_id"),
					true, getActivity());
			status.execute();
			return true;

		case R.id.menu_note_mark_asopen:

			status = note.new NoteToggleStatus(mArgument.getInt("note_id"),
					false, getActivity());
			status.execute();
			return true;

		case R.id.menu_note_edit:
			Intent manageNote = new Intent(getActivity(),
					NoteComposeActivity.class);
			Bundle noteArgs = new Bundle();
			noteArgs.putInt("note_id", mArgument.getInt("note_id"));
			manageNote.putExtras(noteArgs);
			startActivity(manageNote);
			return true;

		case R.id.menu_note_delete:
			confirmDeleteNote(mArgument.getInt("note_id"));
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void confirmDeleteNote(final int id) {
		AlertDialog.Builder deleteDialogConfirm = new AlertDialog.Builder(
				getActivity());
		deleteDialogConfirm.setTitle("Delete");
		deleteDialogConfirm.setMessage("Are you sure want to delete ?");
		deleteDialogConfirm.setCancelable(true);

		deleteDialogConfirm.setPositiveButton("Delete",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						OEHelper oe = db().getOEInstance();
						if (oe != null) {
							oe.delete(id);
							getActivity().getSupportFragmentManager()
									.popBackStack();
						} else {
							Toast.makeText(getActivity(),
									"Operation aborted. No connection.",
									Toast.LENGTH_LONG).show();
						}

					}
				});

		deleteDialogConfirm.setNegativeButton("Cancel", null);
		deleteDialogConfirm.show();
	}

	@Override
	public void onResume() {
		super.onResume();
		showNoteDetails(getArguments().getInt("note_id"));
	}

	@Override
	public Object databaseHelper(Context context) {
		return new NoteDB(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		return null;
	}

}
