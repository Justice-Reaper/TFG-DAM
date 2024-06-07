package com.optic.gamerhub.activities;

import static com.optic.gamerhub.activities.HomeActivity.SELECTED_FRAGMENT;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.UploadTask;
import com.optic.gamerhub.R;
import com.optic.gamerhub.fragments.ProfileFragment;
import com.optic.gamerhub.models.User;
import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.ImageProvider;
import com.optic.gamerhub.providers.UsersProvider;
import com.optic.gamerhub.utils.FileUtil;
import com.optic.gamerhub.utils.ViewedMessageHelper;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;

public class EditProfileActivity extends AppCompatActivity {

    // Declaración de variables
    CircleImageView mCircleImageViewBack;
    CircleImageView mCircleImageViewProfile;
    ImageView mImageViewCover;
    TextInputEditText mTextInputUsername;
    TextInputEditText mTextInputPhone;
    Button mButtonEditProfile;

    AlertDialog.Builder mBuilderSelector;
    CharSequence options[];
    private final int GALLERY_REQUEST_CODE_PROFILE = 1;
    private final int GALLERY_REQUEST_CODE_COVER = 2;
    private final int PHOTO_REQUEST_CODE_PROFILE = 3;
    private final int PHOTO_REQUEST_CODE_COVER = 4;

    // FOTO 1
    String mAbsolutePhotoPath;
    String mPhotoPath;
    File mPhotoFile;

    // FOTO 2
    String mAbsolutePhotoPath2;
    String mPhotoPath2;
    File mPhotoFile2;

    File mImageFile;
    File mImageFile2;

    String mUsername = "";
    String mPhone = "";
    String mImageProfile = "";
    String mImageCover = "";

    AlertDialog mDialog;

    ImageProvider mImageProvider;
    UsersProvider mUsersProvider;
    AuthProvider mAuthProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Inicializar vistas
        mCircleImageViewBack = findViewById(R.id.circleImageBack);
        mCircleImageViewProfile = findViewById(R.id.circleImageProfile);
        mImageViewCover = findViewById(R.id.imageViewCover);
        mTextInputUsername = findViewById(R.id.textInputUsername);
        mTextInputPhone = findViewById(R.id.textInputPhone);
        mButtonEditProfile = findViewById(R.id.btnEditProfile);

        // Configurar el selector de opciones para elegir entre galería y cámara
        mBuilderSelector = new AlertDialog.Builder(this);
        mBuilderSelector.setTitle("Selecciona una opcion");
        options = new CharSequence[] {"Imagen de galeria", "Tomar foto"};

        // Inicializar proveedores y autenticación
        mImageProvider = new ImageProvider();
        mUsersProvider = new UsersProvider();
        mAuthProvider = new AuthProvider();

        // Inicializar diálogo de carga
        mDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Espere un momento")
                .setCancelable(false).build();

        // Verificar y establecer la imagen de perfil si está vacía
        if (mCircleImageViewProfile.getDrawable() == null) {
            String imageMode = getIntent().getStringExtra("image_mode");
            if (Objects.equals(imageMode, "dark")) {
                mCircleImageViewProfile.setImageResource(R.drawable.ic_person); // Cambia aquí por el ID de tu drawable para modo oscuro
            }
            else if (Objects.equals(imageMode, "light")){
                mCircleImageViewProfile.setImageResource(R.drawable.ic_person_grey); // Cambia aquí por el ID de tu drawable para modo claro
            }
        }

        // Listener para el botón de editar perfil
        mButtonEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickEditProfile();

            }
        });

        // Listener para la imagen de perfil
        mCircleImageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectOptionImage(1);
            }
        });

        // Listener para la imagen de portada
        mImageViewCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectOptionImage(2);
            }
        });

        // Listener para el botón de retroceso
        mCircleImageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Obtener información del usuario
        getUser();
    }

    private void getUser() {
        // Obtener información del usuario actual
        mUsersProvider.getUser(mAuthProvider.getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    // Verificar y establecer el nombre de usuario
                    if (documentSnapshot.contains("username")) {
                        mUsername = documentSnapshot.getString("username");
                        mTextInputUsername.setText(mUsername);
                    }

                    // Verificar y establecer el nombre de usuario
                    if (documentSnapshot.contains("phone")) {
                        mPhone = documentSnapshot.getString("phone");
                        mTextInputPhone.setText(mPhone);
                    }

                    // Verificar y establecer la imagen de perfil
                    if (documentSnapshot.contains("image_profile")) {
                        mImageProfile = documentSnapshot.getString("image_profile");
                        if (mImageProfile != null) {
                            if (!mImageProfile.isEmpty()) {
                                Picasso.with(EditProfileActivity.this).load(mImageProfile).into(mCircleImageViewProfile);
                            }
                        }
                    }

                    // Verificar y establecer la imagen de portada
                    if (documentSnapshot.contains("image_cover")) {
                        mImageCover = documentSnapshot.getString("image_cover");
                        if (mImageCover != null) {
                            if (!mImageCover.isEmpty()) {
                                Picasso.with(EditProfileActivity.this).load(mImageCover).into(mImageViewCover);
                            }
                        }
                    }
                }
            }
        });
    }

    private void clickEditProfile() {
        // Obtener el nombre de usuario y el número de teléfono ingresados por el usuario
        mUsername = Objects.requireNonNull(mTextInputUsername.getText()).toString();
        mPhone = Objects.requireNonNull(mTextInputPhone.getText()).toString();

        // Verificar si se han seleccionado nuevas imágenes
        boolean isImageSelected = mPhotoFile != null || mPhotoFile2 != null || mImageFile != null || mImageFile2 != null;

        if (!mUsername.isEmpty() && !mPhone.isEmpty()) {
            if (isImageSelected) {
                // Si se han seleccionado nuevas imágenes, guardarlas y luego actualizar la información del usuario
                if (mImageFile != null && mImageFile2 != null ) {
                    saveImageCoverAndProfile(mImageFile, mImageFile2);
                }
                else if (mPhotoFile != null && mPhotoFile2 != null) {
                    saveImageCoverAndProfile(mPhotoFile, mPhotoFile2);
                }
                else if (mImageFile != null && mPhotoFile2 != null) {
                    saveImageCoverAndProfile(mImageFile, mPhotoFile2);
                }
                else if (mPhotoFile != null && mImageFile2 != null) {
                    saveImageCoverAndProfile(mPhotoFile, mImageFile2);
                }
                else if (mPhotoFile != null) {
                    saveImage(mPhotoFile, true);
                }
                else if (mPhotoFile2 != null) {
                    saveImage(mPhotoFile2, false);
                }
                else if (mImageFile != null) {
                    saveImage(mImageFile, true);
                }
                else if (mImageFile2 != null) {
                    saveImage(mImageFile2, false);
                }
            }

            else {
                // Si no se han seleccionado nuevas imágenes, solo actualizar la información del usuario
                User user = new User();
                user.setUsername(mUsername.toLowerCase());
                user.setPhone(mPhone);
                user.setImageProfile(mImageProfile);  // Mantener la imagen de perfil actual
                user.setImageCover(mImageCover);      // Mantener la imagen de portada actual
                user.setId(mAuthProvider.getUid());
                updateInfo(user);

            }
        } else {
            // Si no se han ingresado datos, mostrar un mensaje de advertencia
            Toast.makeText(this, "No se ha modificado ningún campo", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageCoverAndProfile(File imageFile1, final File imageFile2) {
        // Verificar si la actividad aún está en un estado válido
        if (!isFinishing()) {
            mDialog.show();
        }

        // Guardar la primera imagen (imagen de perfil)
        mImageProvider.save(EditProfileActivity.this, imageFile1).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    // Obtener la URL de descarga de la primera imagen (imagen de perfil)
                    mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            final String urlProfile = uri.toString();

                            // Guardar la segunda imagen (imagen de portada)
                            mImageProvider.save(EditProfileActivity.this, imageFile2).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> taskImage2) {

                                    // Verificar si la actividad aún está en un estado válido antes de despedir el diálogo de carga
                                    if (!isFinishing()) {
                                        mDialog.dismiss();
                                    }

                                    // Obtener la URL de descarga de la segunda imagen (imagen de portada)
                                    if (taskImage2.isSuccessful()) {
                                        mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri2) {
                                                String urlCover = uri2.toString();

                                                // Crear un objeto User con la información actualizada
                                                User user = new User();
                                                user.setUsername(mUsername);
                                                user.setPhone(mPhone);
                                                user.setImageProfile(urlProfile);
                                                user.setImageCover(urlCover);
                                                user.setId(mAuthProvider.getUid());

                                                // Actualizar la información del usuario
                                                updateInfo(user);
                                            }
                                        });
                                    } else {
                                        Toast.makeText(EditProfileActivity.this, "La imagen numero 2 no se pudo guardar", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
                } else {
                    // Despedir el diálogo de carga y mostrar un mensaje de error si no se pudo guardar la primera imagen
                    mDialog.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Hubo error al almacenar la imagen", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveImage(File image, final boolean isProfileImage) {
        // Verificar si la actividad aún está en un estado válido
        if (!isFinishing()) {
            mDialog.show();
        }

        // Guardar la imagen en Firebase Storage
        mImageProvider.save(EditProfileActivity.this, image).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                // Verificar si la actividad aún está en un estado válido
                if (!isFinishing()) {
                    mDialog.dismiss();
                }

                // Verificar si la operación fue exitosa
                if (task.isSuccessful()) {
                    // Obtener la URL de descarga de la imagen
                    mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            final String url = uri.toString();

                            // Crear un objeto User con la información actualizada
                            User user = new User();
                            user.setUsername(mUsername);
                            user.setPhone(mPhone);
                            if (isProfileImage) {
                                user.setImageProfile(url);
                                user.setImageCover(mImageCover);
                            } else {
                                user.setImageCover(url);
                                user.setImageProfile(mImageProfile);
                            }
                            user.setId(mAuthProvider.getUid());

                            // Actualizar la información del usuario
                            updateInfo(user);
                        }
                    });
                } else {
                    // Mostrar un mensaje de error si no se pudo guardar la imagen
                    Toast.makeText(EditProfileActivity.this, "Hubo error al almacenar la imagen", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateInfo(User user) {
        // Verificar si el diálogo de carga está mostrándose antes de intentar mostrarlo nuevamente
        if (mDialog.isShowing()) {
            mDialog.show();
        }

        // Actualizar la información del usuario en la base de datos Firestore
        mUsersProvider.update(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // Despedir el diálogo de carga después de que la operación haya finalizado
                mDialog.dismiss();

                // Verificar si la actualización fue exitosa
                if (task.isSuccessful()) {
                    recargarFragmento();
                }
                else {
                    Toast.makeText(EditProfileActivity.this, "La informacion no se pudo actualizar", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void recargarFragmento() {
        Intent intent = new Intent(EditProfileActivity.this, HomeActivity.class);
        // Establece el fragmento seleccionado a ProfileFragment
        intent.putExtra(SELECTED_FRAGMENT, ProfileFragment.class.getName());
        // Inicia una nueva actividad
        startActivity(intent);
        // Finaliza la actividad actual
        finish();
    }


    private void selectOptionImage(final int numberImage) {
        // Configurar el diálogo de selección de opciones
        mBuilderSelector.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    if (numberImage == 1) {
                        openGallery(GALLERY_REQUEST_CODE_PROFILE);
                    }
                    else if (numberImage == 2) {
                        openGallery(GALLERY_REQUEST_CODE_COVER);
                    }
                }
                else if (i == 1){
                    if (numberImage == 1) {
                        takePhoto(PHOTO_REQUEST_CODE_PROFILE);
                    }
                    else if (numberImage == 2) {
                        takePhoto(PHOTO_REQUEST_CODE_COVER);
                    }
                }
            }
        });

        // Mostrar el diálogo de selección de opciones
        mBuilderSelector.show();

    }

    private void takePhoto(int requestCode) {
        // Crear un intent para capturar una imagen con la cámara
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Verificar si hay una aplicación que pueda manejar la captura de imágenes
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Crear un archivo para almacenar la imagen capturada
            File photoFile = null;
            try {
                photoFile = createPhotoFile(requestCode);
            } catch(Exception e) {
                Toast.makeText(this, "Hubo un error con el archivo " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            // Si se creó correctamente el archivo
            if (photoFile != null) {
                // Obtener la URI del archivo para la imagen capturada
                Uri photoUri = FileProvider.getUriForFile(EditProfileActivity.this, "com.optic.gamerhub", photoFile);
                // Asignar la URI del archivo al intent para que la imagen capturada se guarde allí
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                // Iniciar la actividad de la cámara para capturar la imagen
                startActivityForResult(takePictureIntent, requestCode);
            }
        }
    }

    private File createPhotoFile(int requestCode) throws IOException {
        // Obtener el directorio de almacenamiento externo para las imágenes
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Crear un archivo temporal para la imagen con un nombre único
        File photoFile = File.createTempFile(
                new Date() + "_photo",
                ".jpg",
                storageDir
        );

        // Si el requestCode indica que se está creando un archivo para la imagen de perfil
        if (requestCode == PHOTO_REQUEST_CODE_PROFILE) {
            // Almacenar la ruta de la imagen de perfil
            mPhotoPath = "file:" + photoFile.getAbsolutePath();
            mAbsolutePhotoPath = photoFile.getAbsolutePath();
        }

        // Si el requestCode indica que se está creando un archivo para la imagen de portada
        else if (requestCode == PHOTO_REQUEST_CODE_COVER) {
            // Almacenar la ruta de la imagen de portada
            mPhotoPath2 = "file:" + photoFile.getAbsolutePath();
            mAbsolutePhotoPath2 = photoFile.getAbsolutePath();
        }

        // Devolver el archivo creado
        return photoFile;
    }

    private void openGallery(int requestCode) {
        // Crear un intent para abrir la galería de imágenes
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        // Establecer el tipo de contenido como imágenes
        galleryIntent.setType("image/*");
        // Iniciar la actividad para seleccionar una imagen de la galería
        startActivityForResult(galleryIntent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Selección de imagen desde la galería
        if (requestCode == GALLERY_REQUEST_CODE_PROFILE && resultCode == RESULT_OK) {
            try {
                mPhotoFile = null;
                assert data != null;
                mImageFile = FileUtil.from(this, data.getData());
                mCircleImageViewProfile.setImageBitmap(BitmapFactory.decodeFile(mImageFile.getAbsolutePath()));
            } catch(Exception e) {
                Log.d("ERROR", "Se produjo un error " + e.getMessage());
                Toast.makeText(this, "Se produjo un error " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == GALLERY_REQUEST_CODE_COVER && resultCode == RESULT_OK) {
            try {
                mPhotoFile2 = null;
                assert data != null;
                mImageFile2 = FileUtil.from(this, data.getData());
                mImageViewCover.setImageBitmap(BitmapFactory.decodeFile(mImageFile2.getAbsolutePath()));
            } catch(Exception e) {
                Log.d("ERROR", "Se produjo un error " + e.getMessage());
                Toast.makeText(this, "Se produjo un error " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        // Selección de fotografía
        if (requestCode == PHOTO_REQUEST_CODE_PROFILE && resultCode == RESULT_OK) {
            mImageFile = null;
            mPhotoFile = new File(mAbsolutePhotoPath);
            Picasso.with(EditProfileActivity.this).load(mPhotoPath).into(mCircleImageViewProfile);
        }

        // Captura de fotografía
        if (requestCode == PHOTO_REQUEST_CODE_COVER && resultCode == RESULT_OK) {
            mImageFile2 = null;
            mPhotoFile2 = new File(mAbsolutePhotoPath2);
            Picasso.with(EditProfileActivity.this).load(mPhotoPath2).into(mImageViewCover);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Actualizar estado en línea cuando la actividad está en primer plano
        ViewedMessageHelper.updateOnline(true, EditProfileActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Actualizar estado fuera de línea cuando la actividad está en segundo plano
        ViewedMessageHelper.updateOnline(false, EditProfileActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancelar la tarea asíncrona si aún está en curso
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        if (mImageProvider != null) {
            mImageProvider.cancelTasks();
        }
    }

    // Método para lanzar el fragmento ProfileFragment
    private void launchProfileFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        ProfileFragment fragment = new ProfileFragment();
        transaction.replace(androidx.appcompat.R.id.home, fragment, "ProfileFragment");
        transaction.addToBackStack(null);
        transaction.commit();
    }


}
