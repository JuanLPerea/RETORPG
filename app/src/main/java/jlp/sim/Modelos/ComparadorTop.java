package jlp.sim.Modelos;

import java.util.Comparator;

/**
 * Created by aerocool on 29/07/17.
 */

public class ComparadorTop implements Comparator<Jugador> {

    @Override
    public int compare(Jugador jugador1, Jugador jugador2) {

        int sumapuntosJ1 = jugador1.getFuerza() + jugador1.getInteligencia() + jugador1.getMagia();
        int sumapuntosJ2 = jugador2.getFuerza() + jugador2.getInteligencia() + jugador2.getMagia();

        int compare = (sumapuntosJ1 > sumapuntosJ2) ? 1 : 0;

        return compare;
    }
}
