package com.weefer.opencv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AdapterPerson extends BaseAdapter {

    private MainFace activity;
    private List<Person> lPerson;

    public AdapterPerson(MainFace activity) {
        this.activity = activity;
        lPerson = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return lPerson.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void addPerson(Person mPerson) {
        lPerson.add(mPerson);
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Person setPerson = lPerson.get(position);

        ViewHolder mHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
            mHolder = new ViewHolder(convertView);
            convertView.setTag(mHolder);
        } else {
            mHolder = (ViewHolder) convertView.getTag();
        }

        mHolder.name.setText(setPerson.getName());
        mHolder.image.setImageBitmap(setPerson.getImage());

        mHolder.update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.ClickUpdate(position);
            }
        });
        mHolder.delete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                activity.ClickDelete(position);
                return false;
            }
        });

        return convertView;
    }

    public Person getPerson(int position) {
        return lPerson.get(position);
    }

    public class ViewHolder {

        ImageView image;
        TextView name;
        TextView update;
        LinearLayout delete;

        ViewHolder(View view) {
            update = view.findViewById(R.id.update);
            delete = view.findViewById(R.id.delete);
            image = view.findViewById(R.id.image);
            name = view.findViewById(R.id.txt_nama);
        }
    }
}