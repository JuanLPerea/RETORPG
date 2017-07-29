package jlp.sim.Modelos;


import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import jlp.sim.LoginActivity;

/**
 * Created by aerocool on 24/06/17.
 */

public class Jugador implements Comparable<Jugador> {

    private String id;
    private String nombre;
    private int fuerza;
    private int inteligencia;
    private int magia;
    private int proteccion;
    private int victorias;
    private int derrotas;
    private List<String> mensajes;
    private List<String> amigos;
    private boolean jugando;
    private boolean completado;
    private long tiemporeto;
    private int resultadoreto;
    private int resultadorival;
    private boolean conectado;
    private String actualizado;
    private String retadorId;
    private String rivalId;
    private String ultimoReto;
    private String notificacion;


    public Jugador() {
    }

    public Jugador(String nombre, String uid) {
        this.nombre = nombre;
        this.jugando = false;
        this.completado = false;
        this.id = uid;
        this.retadorId = "*";
        this.resultadoreto = 0;
        this.resultadorival = 0;
        this.ultimoReto = "";
        this.notificacion = "";

        setActualizado();

        mensajes = new ArrayList<>();
        mensajes.add("Hola soy nuevo!!");

        amigos = new ArrayList<>();
        amigos.add(this.getId());


    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getFuerza() {
        return fuerza;
    }

    public void setFuerza(int fuerza) {
        this.fuerza = fuerza;
    }

    public int getInteligencia() {
        return inteligencia;
    }

    public void setInteligencia(int inteligencia) {
        this.inteligencia = inteligencia;
    }

    public int getMagia() {
        return magia;
    }

    public void setMagia(int magia) {
        this.magia = magia;
    }

    public List<String> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<String> mensajes) {
        this.mensajes = mensajes;
    }

    public boolean isJugando() {
        return jugando;
    }

    public void setJugando(boolean jugando) {
        this.jugando = jugando;
    }

    public boolean isCompletado() {
        return completado;
    }

    public void setCompletado(boolean completado) {
        this.completado = completado;
    }

    public long getTiemporeto() {
        return tiemporeto;
    }

    public void setTiemporeto(long tiemporeto) {
        this.tiemporeto = tiemporeto;
    }

    public int getResultadoreto() {
        return resultadoreto;
    }

    public void setResultadoreto(int resultadoreto) {
        this.resultadoreto = resultadoreto;
    }

    public List<String> getAmigos() {
        return amigos;
    }

    public void addAmigos(String amigoID) {

        int repetido = 0;

        for (String tmp : amigos) {
            if (tmp == amigoID) repetido = 1;
        }

        if (repetido == 0) amigos.add(amigoID);

    }

    public int getProteccion() {
        return proteccion;
    }

    public void setProteccion(int proteccion) {
        this.proteccion = proteccion;
    }

    public int getVictorias() {
        return victorias;
    }

    public void setVictorias(int victorias) {
        this.victorias = victorias;
    }

    public int getDerrotas() {
        return derrotas;
    }

    public void setDerrotas(int derrotas) {
        this.derrotas = derrotas;
    }

    public boolean isConectado() {
        return conectado;
    }

    public void setConectado(boolean conectado) {
        this.conectado = conectado;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void mensajeAdd(String msg) {
        mensajes.add(msg);
        if (mensajes.size() >= 100) mensajes.removeAll(mensajes);
    }

    public String getActualizado() {
        return actualizado;
    }

    public void setActualizado() {
        Calendar calendario = Calendar.getInstance();

        String ahora = calendario.getTimeInMillis() + "";
        // Log.d("ACTUALIZADO", ahora);
        this.actualizado = ahora;
    }


    public int compareTo(Jugador compararJugador) {

        String compararactualizado = ((Jugador) compararJugador).getActualizado();

        String jugadoractualizado = this.getActualizado();


        // Log.d("COMPARAR", "" + compararactualizado.compareTo(jugadoractualizado));
        // Orden ascendente
        return compararactualizado.compareTo(jugadoractualizado);
    }


    public String getRetadorId() {
        return retadorId;
    }

    public void setRetadorId(String retadorId) {
        this.retadorId = retadorId;
    }

    public String getRivalId() {
        return rivalId;
    }

    public void setRivalId(String rivalId) {
        this.rivalId = rivalId;
    }

    public int getResultadorival() {
        return resultadorival;
    }

    public void setResultadorival(int resultadorival) {
        this.resultadorival = resultadorival;
    }

    public String getUltimoReto() {
        return ultimoReto;
    }

    public void setUltimoReto(String ultimoReto) {
        this.ultimoReto = ultimoReto;
    }

    public String getNotificacion() {
        return notificacion;
    }

    public void setNotificacion(String notificacion) {
        this.notificacion = notificacion;
    }
}
