package jlp.sim;

import android.*;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jlp.sim.Adapters.ItemClickSupport;
import jlp.sim.Adapters.PlayersAdapter;
import jlp.sim.Modelos.ComparadorTop;
import jlp.sim.Modelos.Jugador;
import jlp.sim.Modelos.ServicioNotificacion;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kosalgeek.android.photoutil.CameraPhoto;
import com.kosalgeek.android.photoutil.GalleryPhoto;
import com.kosalgeek.android.photoutil.ImageLoader;

public class MainActivity extends AppCompatActivity {

    RecyclerView rv;
    List<Jugador> players;
    Jugador jugadorActivo;
    Jugador rival;
    PlayersAdapter playersAdapter;

    EditText mensajetxt, buscajugador;
    TextView nombreJugador, fuerzaTV, inteligenciaTV, magiaTV;
    RadioGroup radioGroup;
    ImageView imagenJugador;

    FirebaseDatabase database;
    FirebaseUser user;
    FirebaseAuth.AuthStateListener mAuthListener;
    Context context = this;
    Bundle analyticsBundle;
    DatabaseReference userRef;
    DatabaseReference jugadoresRef;
    DatabaseReference rivalRef;

    // Create a storage reference from our app
    StorageReference storageRef;
    FirebaseStorage storage;


    int filtrar = 1;
    private FirebaseAuth auth;
    private FirebaseAnalytics mFirebaseAnalytics;
    final int MAX_JUGADORES = 100;
    final int ACTIVITY_SELECT_IMAGE = 1234;

    // Para la foto
    CameraPhoto cameraPhoto;
    final int CAMERA_REQUEST = 1100;
    Drawable fotodrawable;
    GalleryPhoto galleryPhoto;
    final int GALLERY_REQUEST = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Views del Activity
        mensajetxt = (EditText) findViewById(R.id.textomensajeET);
        nombreJugador = (TextView) findViewById(R.id.textJugador);
        fuerzaTV = (TextView) findViewById(R.id.strtxt);
        inteligenciaTV = (TextView) findViewById(R.id.inttxt);
        magiaTV = (TextView) findViewById(R.id.magtxt);
        radioGroup = (RadioGroup) findViewById(R.id.grupoBTN);
        buscajugador = (EditText) findViewById(R.id.busca_jugadorET);
        imagenJugador = (ImageView) findViewById(R.id.logoJugador);

        // Recycler View Para la lista de jugadores
        rv = (RecyclerView) findViewById(R.id.recycler_players);
        rv.setLayoutManager(new LinearLayoutManager(this));
        players = new ArrayList<>();
        playersAdapter = new PlayersAdapter(context, players);
        rv.setAdapter(playersAdapter);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        auth = FirebaseAuth.getInstance();
        analyticsBundle = new Bundle();

        // Referencias a Firebase
        database = FirebaseDatabase.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        userRef = database.getReference().child(user.getUid());
        jugadoresRef = database.getReference().getRoot();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://simgame-41469.appspot.com");


        // Para obtener una foto
        cameraPhoto = new CameraPhoto(getApplicationContext());
        galleryPhoto = new GalleryPhoto(getApplicationContext());

        // Chequear permisos
        checkPermission();

        // Bajar la imágen del usuario de Firebase Storage
        descargarImagen();

        // Iniciar el servicio de notificaciones
        notificacion();

        // Si no estamos autenticados, lanzar la activity de login
        if (auth.getCurrentUser() == null) {
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
        }

        // Listener para actualizar nuestro recycler View cuando cambia algún dato en Firebase (Real Time)
        jugadoresRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                players.removeAll(players);

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Jugador jugador = snapshot.getValue(Jugador.class);

                    // si alguien te reta o has retado a alguien, detectarlo aqui y recuperar los datos del rival
                    if (jugadorActivo!=null) {
                        if (jugador.getId().equals(jugadorActivo.getRivalId()) && jugadorActivo.isJugando() == false) {
                            rival = jugador;
                            rival.setJugando(true);
                            rival.setRivalId(jugadorActivo.getId());

                            jugadorActivo.setRivalId(rival.getId());
                            jugadorActivo.setJugando(true);
                            userRef.setValue(jugadorActivo);

                            // lanzar el diálogo
                            retodialog(rival);

                        }


                        // Llenar la lista para el Recycler View
                        switch (filtrar) {
                            // Caso de querer ver a los mejores jugadores
                            case 1:
                                players.add(jugador);
                                break;

                            case 2:
                                for (String tmp : jugadorActivo.getAmigos()) {
                                    Log.d("AMIGOS", tmp);
                                    if (jugador.getId().equals(tmp)) {
                                        players.add(jugador);
                                    }
                                }
                                break;

                            case 3:

                                if (jugadorActivo.getFuerza() + jugadorActivo.getInteligencia() + jugadorActivo.getMagia()
                                        >= (jugador.getFuerza() + jugador.getInteligencia() + jugador.getMagia()) - 10 &&
                                        jugadorActivo.getFuerza() + jugadorActivo.getInteligencia() + jugadorActivo.getMagia()
                                                <= (jugador.getFuerza() + jugador.getInteligencia() + jugador.getMagia()) + 10
                                        ) {
                                    players.add(jugador);
                                }
                                break;

                            case 4:
                                if (jugador.isConectado()) players.add(jugador);
                                break;

                            case 5:
                                // mostrar en la lista solo el jugador que indique en el campo de texto

                                String buscar = buscajugador.getText().toString();


                                if (jugador.getNombre().contains(buscar)) {
                                    players.add(jugador);
                                }
                                break;
                        }

                    }
                }



                // Si queremos ver los mejores
                if (filtrar == 1) {
                    Collections.sort(players, new ComparadorTop());
                }

                // ordenar lista por el campo "Actualizado"
                Collections.sort(players);

                // si hay mas de 100 registros eliminar hasta que haya 100
                while (players.size()>100){
                    players.remove(players.size()-1);
                }



                // Actualizar el Recycler View
                playersAdapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        // recuperar el jugador activo
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot2) {
                jugadorActivo = dataSnapshot2.getValue(Jugador.class);

                if (jugadorActivo != null) {
                    nombreJugador.setText(jugadorActivo.getNombre());
                    fuerzaTV.setText(jugadorActivo.getFuerza() + "");
                    inteligenciaTV.setText(jugadorActivo.getInteligencia() + "");
                    magiaTV.setText(jugadorActivo.getMagia() + "");
                    descargarImagen();

                    jugadorActivo.setConectado(true);
                    jugadorActivo.setActualizado();

                    // detectar aquí si tenemos el resultado del reto propio y del rival
                    // si es así mostrar el diálogo de fin de reto
                    if (jugadorActivo.getResultadoreto() != 0 && jugadorActivo.getResultadorival() != 0) {
                        finretodialog();
                    }


                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        // Iniciar reto con un rival al hacer click en el Recycler View
        // Item Click Listener
        ItemClickSupport.addTo(rv).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {

                rival = players.get(position);
                rivalRef = database.getReference().child(rival.getId());

                analyticsBundle.putString(FirebaseAnalytics.Event.SIGN_UP, jugadorActivo.getNombre() + "");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, analyticsBundle);

                // Si tocamos a un jugador que no está conectado le mandamos una notificación
                if (!rival.isConectado()) {
                    Toast.makeText(context, "El Rival tiene que estar conectado, mandado mensaje para que se conecte...", Toast.LENGTH_LONG).show();
                    rival.setNotificacion("Te ha retado " + jugadorActivo.getNombre() +"\n¡¡¡ Conéctate a S.I.M para aceptar el Reto !!!");
                    rivalRef.setValue(rival);
                }

                // Si el jugador seleccionado está online y es distinto del propio jugador activo mostrar dialogo
                if (jugadorActivo.getId() != rival.getId() && rival.isConectado()) {

                    if (rival.isJugando() == false) {
                        // Actualizar el jugador para que sepa quien es su rival con el campo
                        jugadorActivo.setRivalId(rival.getId());
                        userRef.setValue(jugadorActivo);

                        // Actualizar el estado del rival que has retado para que le aparezca una notificación

                        rival.setRivalId(jugadorActivo.getId());
                        rivalRef.setValue(rival);

                        //retodialog(rival);
                    } else {
                        Toast.makeText(context, "El jugador ya está retado", Toast.LENGTH_SHORT).show();
                    }


                }

            }
        });


        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                switch (i) {
                    case R.id.topBTN:
                        filtrar = 1;
                        break;

                    case R.id.friendsBTN:
                        filtrar = 2;
                        break;

                    case R.id.equalsBTN:
                        filtrar = 3;
                        break;

                    case R.id.lastBTN:
                        filtrar = 4;
                        break;
                }


                jugadorActivo.setActualizado();
                userRef.setValue(jugadorActivo);
            }

        });


    }
/*
    @Override
    protected void onResume() {
        super.onResume();
        // Log.d("MENSAJE", "ON RESUME");

        if (jugadorActivo != null) {
            jugadorActivo.setConectado(true);
            userRef.setValue(jugadorActivo);
        }
    }
*/

    @Override
    protected void onStart() {
        super.onStart();
        descargarImagen();
        if (jugadorActivo != null) {
            jugadorActivo.setConectado(true);
            userRef.setValue(jugadorActivo);
        }
    }

    // Menú para desconectar el usuario
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.desconectar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.desconectar_btn:
                jugadorActivo.setConectado(false);
                userRef.setValue(jugadorActivo);
                auth.signOut();

                Intent i = new Intent(this, LoginActivity.class);
                startActivity(i);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void enviarmensaje(View vista) {

        jugadorActivo.setConectado(true);
        jugadorActivo.setActualizado();
        jugadorActivo.mensajeAdd(mensajetxt.getText().toString());
        userRef.setValue(jugadorActivo);
        mensajetxt.setText("");

    }


    @Override
    protected void onPause() {
        super.onPause();

         Log.d("MENSAJE", "ON PAUSE");

        if (jugadorActivo != null) {
            jugadorActivo.setConectado(false);
            userRef.setValue(jugadorActivo);
        }


    }

    @Override
    protected void onStop() {
        super.onStop();

         Log.d("MENSAJE", "ON STOP");

        if (jugadorActivo != null) {
            jugadorActivo.setConectado(false);
            userRef.setValue(jugadorActivo);
        }

    }


    // si el usuario pulsa el botón back del dispositivo
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Log.d("MENSAJE", "BACK PRESIONADO");


    }

    public void retodialog(final Jugador rivaldlg) {

        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialogo_reto);
        dialog.setTitle("S I M");

        final ImageView imagenrivalIV = (ImageView) dialog.findViewById(R.id.imagenrivaldialogIV);
        final TextView nombrerivaltxt = (TextView) dialog.findViewById(R.id.nombrerivaldialogTV);
        final TextView fuerzarivaltxt = (TextView) dialog.findViewById(R.id.fuerzadialogTV);
        final TextView inteligenciarivaltxt = (TextView) dialog.findViewById(R.id.inteligenciadialogTV);
        final TextView magiarivaltxt = (TextView) dialog.findViewById(R.id.magiadialogTV);
        final ImageButton fuerzaBTN = (ImageButton) dialog.findViewById(R.id.fuerzadialogBTN);
        final ImageButton inteliBTN = (ImageButton) dialog.findViewById(R.id.inteligdialogBTN);
        final ImageButton magiaBTN = (ImageButton) dialog.findViewById(R.id.magiadialogBTN);
        final ImageButton amigoBTN = (ImageButton) dialog.findViewById(R.id.amigoBTN);


        //imagenrivalIV.setImageResource();
        nombrerivaltxt.setText(rivaldlg.getNombre());
        fuerzarivaltxt.setText(rivaldlg.getFuerza() + "");
        inteligenciarivaltxt.setText(rivaldlg.getInteligencia() + "");
        magiarivaltxt.setText(rivaldlg.getMagia() + "");


        // Añadir como amigo
        amigoBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                jugadorActivo.addAmigos(rivaldlg.getId());
                Toast.makeText(context, "Jugador añadido como Amigo", Toast.LENGTH_SHORT).show();
                userRef.setValue(jugadorActivo);
            }
        });


        // Retar usando Fuerza
        fuerzaBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                Intent myintent2 = new Intent(context, FuerzaActivity.class);
                startActivityForResult(myintent2, 1);
            }
        });

        // Retar usando Inteligencia
        inteliBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                Intent myintent2 = new Intent(context, PuzzleActivity.class);
                startActivityForResult(myintent2, 2);
            }
        });

        // Retar usando Magia
        magiaBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                Intent myintent3 = new Intent(context, MagiaActivity.class);
                startActivityForResult(myintent3, 3);
            }
        });

        // mostrar el diálogo
        dialog.show();
    }


    public void finretodialog() {

        // Ver si hemos ganado o hemos perdido

        Log.d("FINRETO", "" + jugadorActivo.getResultadoreto() + " - " + jugadorActivo.getResultadorival());



        final Dialog dialog_finreto = new Dialog(context);
        dialog_finreto.setContentView(R.layout.dialogo_resultado);
        dialog_finreto.setTitle("S I M");


        final ImageView imagen_jugador_IV = (ImageView) dialog_finreto.findViewById(R.id.resultado_jugador_IV);
        final ImageView imagen_rival_IV = (ImageView) dialog_finreto.findViewById(R.id.resultado_rival_IV);
        final TextView nombre_jugador_resultado_TV = (TextView) dialog_finreto.findViewById(R.id.resultadojugador_nombreTV);
        final TextView nombre_rival_resultado_TV = (TextView) dialog_finreto.findViewById(R.id.resultado_rival_nombreTV);
        final TextView puntos_jugador_resultado_TV = (TextView) dialog_finreto.findViewById(R.id.resultadojugador_puntosTV);
        final TextView puntos_rival_resultado_TV = (TextView) dialog_finreto.findViewById(R.id.resultado_rival_puntosTV);

        final ImageButton resultado_OK_BTN = (ImageButton) dialog_finreto.findViewById(R.id.resultado_OK_BTN);



        if (jugadorActivo.getResultadoreto() < jugadorActivo.getResultadorival()) {
            nombre_jugador_resultado_TV.setText(jugadorActivo.getNombre() + " Has Perdido");
            jugadorActivo.setResultadoreto(-1);
        } else if (jugadorActivo.getResultadoreto() > jugadorActivo.getResultadorival()) {
            nombre_jugador_resultado_TV.setText(jugadorActivo.getNombre() + " Has Ganado");
            jugadorActivo.setResultadoreto(1);
        } else {
            nombre_jugador_resultado_TV.setText(jugadorActivo.getNombre() + " Empate");
            jugadorActivo.setResultadoreto(1);
        }

        // Actualizar las puntuaciones
        switch (jugadorActivo.getUltimoReto()) {
            case "Fuerza":
                jugadorActivo.setFuerza(jugadorActivo.getFuerza() + jugadorActivo.getResultadoreto());
                break;
            case "Inteligencia":
                jugadorActivo.setInteligencia(jugadorActivo.getInteligencia() + jugadorActivo.getResultadoreto());
                break;

            case "Magia":
                jugadorActivo.setMagia(jugadorActivo.getMagia() + jugadorActivo.getResultadoreto());
                break;

            case "Cancelado":
                // si cancelas siempre pierdes
                jugadorActivo.setFuerza(jugadorActivo.getFuerza() - 1);
                jugadorActivo.setInteligencia(jugadorActivo.getInteligencia() - 1);
                jugadorActivo.setMagia(jugadorActivo.getMagia() -1);
                break;
        }

        // evitar negativos
        if (jugadorActivo.getFuerza() < 0) jugadorActivo.setFuerza(0);
        if (jugadorActivo.getInteligencia() < 0 ) jugadorActivo.setInteligencia(0);
        if (jugadorActivo.getMagia() < 0 ) jugadorActivo.setMagia(0);


        // mostrar el resultado en el diálogo
        puntos_jugador_resultado_TV.setText(jugadorActivo.getResultadoreto() + "");

        nombre_rival_resultado_TV.setText(rival.getNombre());
        puntos_rival_resultado_TV.setText(jugadorActivo.getResultadorival() + "");


        resultado_OK_BTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // terminamos de dejar preparado el jugador para otro reto y actualizamos la base de datos
                jugadorActivo.setRetadorId("*");
                jugadorActivo.setRivalId("*");
                jugadorActivo.setResultadoreto(0);
                jugadorActivo.setResultadorival(0);
                jugadorActivo.setJugando(false);
                userRef.setValue(jugadorActivo);
                dialog_finreto.dismiss();
            }
        });


        dialog_finreto.show();

    }


    public void buscarJugador(View v){
        filtrar = 5;
        jugadorActivo.setActualizado();
        userRef.setValue(jugadorActivo);
    }


    public void personalizarImg(View v){
        PopupMenu popup = new PopupMenu(context, v);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.menu_foto, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.foto_camara:

                        //call it to open the camera
                        Intent in = null;
                        try {
                            in = cameraPhoto.takePhotoIntent();
                        } catch (IOException e) {
                            Log.e("CAMARA" , e.getMessage());
                        }


                        if (in!=null) {
                            startActivityForResult(in, CAMERA_REQUEST);
                            cameraPhoto.addToGallery();
                        }



                        break;
                    case R.id.foto_galeria:
                        //call it to open the gallery
                        startActivityForResult(galleryPhoto.openGalleryIntent(), GALLERY_REQUEST);

                        break;
                }


                return true;
            }
        });

        popup.show();//showing popup menu

    }



    private boolean checkPermission() {

        boolean permiso = false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            // Toast.makeText(this, "Esta versión de Android no es 6 o superior" + Build.VERSION.SDK_INT, Toast.LENGTH_LONG).show();

        } else {

            int hasCameraContactsPermission = checkSelfPermission(android.Manifest.permission.CAMERA);
            int hasWritePermission = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if(hasWritePermission != PackageManager.PERMISSION_GRANTED){

                requestPermissions(new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, GALLERY_REQUEST);
                permiso = true;

            }


            if (hasCameraContactsPermission != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[] {android.Manifest.permission.CAMERA},
                        CAMERA_REQUEST);

               // Toast.makeText(this, "Solicitando permisos", Toast.LENGTH_LONG).show();
                permiso = true;

            }else if (hasCameraContactsPermission == PackageManager.PERMISSION_GRANTED){

             //   Toast.makeText(this, "Los Permisos ya estaban concedidos ", Toast.LENGTH_LONG).show();
                permiso = true;

            }

        }

        return permiso;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        /*
        Bundle miBundle = new Bundle();


        if (jugadorActivo != null){
            outState.putBundle("JugadorActivo", jugadorActivo);
        }
        */
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // recuperamos el resultado de nuestro reto y lo comunicamos al rival actualizando su base de datos
        // cuando en nuestro jugador están los resultados de nuestro reto y del rival, mostrará el diálogo fin de reto
        // supone que los 2 jugadores han terminado su reto (Se detecta en el listener de la base de datos)

            if (requestCode == 1 || requestCode == 2 || requestCode == 3) {
                if (resultCode == Activity.RESULT_OK) {
                    long puntos = data.getLongExtra("Resultado", -1);
                    String tipo = data.getStringExtra("Juego");

                    jugadorActivo.setUltimoReto(tipo);
                    jugadorActivo.setResultadoreto((int) puntos);

                    rival.setResultadoreto(jugadorActivo.getResultadorival());
                    rival.setResultadorival((int) puntos);

                    // Actualizar la base de datos
                    rivalRef = database.getReference().child(rival.getId());
                    rivalRef.setValue(rival);
                    userRef.setValue(jugadorActivo);

                    Log.d("RESULTADO ", tipo + ": " + puntos + "");

                }
                if (resultCode == Activity.RESULT_CANCELED) {


                    jugadorActivo.setUltimoReto("Cancelado");
                    jugadorActivo.setResultadoreto(-1);

                    rival.setResultadoreto(jugadorActivo.getResultadorival());
                    rival.setResultadorival(-1);

                    // Actualizar la base de datos
                    rivalRef = database.getReference().child(rival.getId());
                    rivalRef.setValue(rival);
                    userRef.setValue(jugadorActivo);

                    Log.d("RESULTADO", "CANCELADO");
                }


                inteligenciaTV.setText(jugadorActivo.getInteligencia() + "");
            }


            // Personalizar Imágen del usuario

            if (resultCode == RESULT_OK) {
                if (requestCode == CAMERA_REQUEST) {
                    String photoPath = cameraPhoto.getPhotoPath();
                    // guardamos en la base de datos la ruta a la foto
                    // cochedetalle.setmImageUri(photoPath);
                    //  cocheReference.setValue(cochedetalle);

                    try {
                        Bitmap bitmap = ImageLoader.init().from(photoPath).requestSize(128, 128).getBitmap();
                        Log.d("IMAGEN", "Camara");
                        subirImagen(bitmap);
                        imagenJugador.setImageBitmap(bitmap); //imageView is your ImageView

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

                if (requestCode == GALLERY_REQUEST) {
                    galleryPhoto.setPhotoUri(data.getData());
                    String photoPath = galleryPhoto.getPath();
                    try {
                        Bitmap bitmap = ImageLoader.init().from(photoPath).requestSize(128, 128).getBitmap();
                        Log.d("IMAGEN", "Galeria");
                        subirImagen(bitmap);
                        imagenJugador.setImageBitmap(bitmap);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

            }

        }


        public void subirImagen(Bitmap bitmap){

            // Borrar el archivo si existe
            // Delete the file
            storageRef.child(user.getUid()).delete().addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) {
                    // File deleted successfully
                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Uh-oh, an error occurred!
                }
            });

            // Get the data from an ImageView as bytes
            // imageView.setDrawingCacheEnabled(true);
            //imageView.buildDrawingCache();
            //Bitmap bitmap = imageView.getDrawingCache();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            UploadTask uploadTask = storageRef.child(user.getUid()).putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                }
            });
        }

        public void descargarImagen(){

            Glide.with(context)
                    .using(new FirebaseImageLoader())
                    .load(storageRef.child(user.getUid()))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(imagenJugador);

        }


    // Servicio para las notificaciones
    public void notificacion() {
        //Iniciar un servicio que a su vez lanza un asyncTask que muestra las notificaciones
        // Servicio de notificaciones
        Log.d("TAREANOTI", "Lanzamos el servicio");
        Intent intentServicioNotificacion = new Intent(this, ServicioNotificacion.class);
        startService(intentServicioNotificacion);
    }


}
