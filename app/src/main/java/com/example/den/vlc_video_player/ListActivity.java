package com.example.den.vlc_video_player;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI;

@RuntimePermissions
public class ListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private String restoreSearchText;     //переменная для сохранения поискового запроса при повороте экрана
    private List<String> filteredList;
    List<String> list;
    AdapterForList adapter;
    Toolbar toolbar;
    SearchView searchView;
    View view;
    private List<String> listName;
    private final int REQUEST_PERMITIONS = 1100;
    private final int REQUEST_PLAYER = 1101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = getLayoutInflater().inflate(R.layout.activity_list, null);
        setContentView(view);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Список видео");// надпись на тулбаре
        setSupportActionBar(toolbar);
        ListActivityPermissionsDispatcher.getPermissionWithPermissionCheck(ListActivity.this);

        if (savedInstanceState != null) {
            restoreSearchText = savedInstanceState.getString("restoreSearchText");
        }
    }

    private void installAdapter() {
        recyclerView = findViewById(R.id.idRecycler);
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    // код для клика по элементу
                    @Override
                    public void onItemClick(View view, int position) {
                        String path = "";
                        String name = "";
                        if (filteredList != null) {
                            for (int i = 0; i < list.size(); i++) {
                                //сравниваем имя, полученное с нажатого элемента, с именем в списке
                                if (listName.get(i).equals(filteredList.get(position))) {
                                    path = list.get(i);
                                    name = listName.get(i);
                                    break;
                                }
                            }
                        } else {
                            path = list.get(position);
                            name = listName.get(position);
                        }
                        Intent intent = new Intent(ListActivity.this, MainActivity.class);
                        intent.putExtra("path", path);
                        intent.putExtra("name", name);
                        intent.putStringArrayListExtra("listPath", (ArrayList<String>) list);
                        intent.putStringArrayListExtra("listName", (ArrayList<String>) listName);
                        startActivityForResult(intent, REQUEST_PLAYER);
                    }//onItemClick

                    //длинное нажатие по элементу
                    @Override
                    public void onLongItemClick(View view, final int position) {

                    }//onLongItemClick
                })//RecyclerItemClickListener
        );

        adapter = new AdapterForList(this, listName);
        recyclerView.setAdapter(adapter);
    }

    // Загрузка меню (пункты "выход" и "добавить пользователя")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);//закачиваем меню из xml
        searchView = (SearchView) menu.findItem(R.id.mySearch).getActionView();
        //в горизонтальной ориентации убираем полноэкранный поиск
        int options = searchView.getImeOptions();//получаем текущие настройки
        searchView.setImeOptions(options | IME_FLAG_NO_EXTRACT_UI);//устанавливаем свои
        searchView.setQueryHint(getResources().getString(R.string.search_hint));//устанавливаем hint надпись в окне поиска
        searchView.setMaxWidth(Integer.MAX_VALUE);
        //слушатель нажатия на иконку поиск
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbar.setTitle("");// надпись на тулбаре
            }
        });
        //слушатель закрытия search окна (нажать на крестик)
        searchView.setOnCloseListener(() -> {//нажатие на крестик (закрыть поиск)
            toolbar.setTitle("Список видео");// надпись на тулбаре
            return false;
        });
        //слушатель изменения текста при поиске
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {//автоматически генерируется
                return true;
            }//onQueryTextSubmit

            @Override
            public boolean onQueryTextChange(String searchText) {
                filteredList = filter(listName, searchText);//создаем отсортированный список
                adapter.animateTo(filteredList);//вызов анимации и удаление элементов в адаптере
                recyclerView.scrollToPosition(0);//ловим изменение текста
                return true;
            }//onQueryTextChange
        });
        if (restoreSearchText != null && !restoreSearchText.equals("")) {//если до поворота экрана в поиске что-то было введено
            searchView.setQuery(restoreSearchText, false);//устанавливаем в строку поиска значение до поворота экрана
            searchView.setIconified(false);//открываем окно поиска
        } else
            searchView.clearFocus();//убираем фокус с строки поиска
        return super.onCreateOptionsMenu(menu);
    }//onCreateOptionsMenu


    //фильтруем по содержанию запроса в фамилии
    private List<String> filter(List<String> list, String searchText) {
        restoreSearchText = searchText;

        List<String> filteredList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            final String textOne = list.get(i);
            if (textOne.toLowerCase(Locale.getDefault()).contains(searchText.toLowerCase(Locale.getDefault()))) {
                filteredList.add(textOne);//добавляем пользователей в отфильтрованный список
            }//if
        }
        return filteredList;
    }//filter


   private void getListVideo() {
        list = new ArrayList<>();
        listName = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor == null) {
            Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show();
            return;
        } else if (!cursor.moveToFirst()) {
            Snackbar.make(view, "На устройстве отсутствует видео файлы", Snackbar.LENGTH_INDEFINITE).show();
            return;
        } else {
            int dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
            int dataColumnName = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
            do {
                String name = cursor.getString(dataColumnName);
                if (name != null) listName.add(name);
                list.add(cursor.getString(dataColumn));
            } while (cursor.moveToNext());
        }
        cursor.close();
        installAdapter();
    }

    //===========================================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_PERMITIONS:
                ListActivityPermissionsDispatcher.getPermissionWithPermissionCheck(ListActivity.this);
                break;
            case REQUEST_PLAYER:
                getListVideo();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }//onActivityResult


    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE})
    void getPermission() {
        getListVideo();
    }


    //refund after agreement / denial of the user
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ListActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }//onRequestPermissionsResult


    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE})
    void permissionsDenied() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_PERMITIONS);
    }//permissionsDenied


    @OnNeverAskAgain({Manifest.permission.READ_EXTERNAL_STORAGE})
    void onNeverAskAgain() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.attention))
                .setIcon(R.mipmap.warning)
                .setMessage(getResources().getString(R.string.need_get_permissions))
                .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .show();
        dialog.setCancelable(false);
    }


    @OnShowRationale({Manifest.permission.READ_EXTERNAL_STORAGE})
    void showRationaleForCamera(final PermissionRequest request) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.need_obtain_permissions))
                .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .show();
        dialog.setCancelable(false);
    }
    //========================================================================================


    //сохраняем данные при повороте экрана
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("saveSearchText", restoreSearchText);//запоминаем текст поиска
        super.onSaveInstanceState(outState);
    }//onSaveInstanceState

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else
            super.onBackPressed();
    }
}
