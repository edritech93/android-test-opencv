package com.weefer.opencv;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.weefer.opencv.utils.Labels;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import at.markushi.ui.CircleButton;

public class MainFace extends AppCompatActivity {

    private GridView gridview;
    private AdapterPerson adapterPerson;
    private Person mPerson;
    private static Labels thelabels;
    private static int count = 0;
    private static Bitmap bmlist[];
    private static String namelist[];
    private static String mPath = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        gridview = findViewById(R.id.gridview);

        CircleButton fb = findViewById(R.id.fab);
        fb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainFace.this, MainActivity.class));
            }
        });

        mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/KodeTR/";
        thelabels = new Labels(mPath);
        thelabels.Read();
        count = 0;
        showPerson();
    }

    public void showPerson() {
        configGridView();
        List<Person> userList = getPerson();
        for (int i = 0; i < userList.size(); i++) {
            mPerson = userList.get(i);
            adapterPerson.addPerson(mPerson);
        }
    }

    public void configGridView() {
        adapterPerson = new AdapterPerson(this);
        gridview.setAdapter(adapterPerson);
    }

    public static List<Person> getPerson() {
        List<Person> wordList = new ArrayList<>();

        int max = thelabels.max();

        for (int i = 0; i <= max; i++) {
            if (thelabels.get(i) != "") {
                count++;
            }
        }

        bmlist = new Bitmap[count];
        namelist = new String[count];
        count = 0;

        for (int i = 0; i <= max; i++) {
            if (thelabels.get(i) != "") {
                File root = new File(mPath);
                final String fname = thelabels.get(i);
                FilenameFilter pngFilter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().startsWith(fname.toLowerCase() + "-");
                    }
                };
                File[] imageFiles = root.listFiles(pngFilter);
                if (imageFiles.length > 0) {
                    InputStream is;
                    try {
                        is = new FileInputStream(imageFiles[0]);

                        bmlist[count] = BitmapFactory.decodeStream(is);
                        namelist[count] = thelabels.get(i);

                        Person mPerson = new Person(namelist[count], bmlist[count]);
                        wordList.add(mPerson);

                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        Log.e("File Erorr", e.getMessage() + " " + e.getCause());
                        e.printStackTrace();
                    }
                }
                count++;
            }
        }
        return wordList;
    }

    private Person selectedPerson;

    public void ClickUpdate(int position) {
        selectedPerson = adapterPerson.getPerson(position);
        Toast.makeText(this, String.valueOf(selectedPerson.getName()), Toast.LENGTH_SHORT).show();
    }

    public void ClickDelete(int position) {
        selectedPerson = adapterPerson.getPerson(position);
        DialogPersonDelete(selectedPerson);
    }

    public void DeletePerson(final Person person) {
        File root = new File(mPath);
        FilenameFilter pngFilter = new FilenameFilter() {
            public boolean accept(File dir, String n) {
                String s = person.getName();
                return n.toLowerCase().startsWith(s.toLowerCase() + "-");
            }
        };
        File[] imageFiles = root.listFiles(pngFilter);
        for (File image : imageFiles) {
            image.delete();
            int i;
            for (i = 0; i < count; i++) {
                if (namelist[i].equalsIgnoreCase(person.getName())) {
                    int j;
                    for (j = i; j < count - 1; j++) {
                        namelist[j] = namelist[j + 1];
                        bmlist[j] = bmlist[j + 1];
                    }
                    count--;
                    showPerson();
                    break;
                }
            }
        }
    }

    public void DialogPersonDelete(final Person person) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Options");
        alertDialog.setMessage("You sure person...");
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        DeletePerson(person);
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_person:
                startActivity(new Intent(MainFace.this, AddActivity.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}