/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ontoSentiment;


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Raimundo
 */
public class Test {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            //Amigos teste = new Amigos();
            //teste.start();
            //Util.imprimeAvaibleTrends();
            Busca busca = new Busca();
            //busca.buscarPorTrendingTopics("Brazil", "pt");
            busca.buscarPorAssunto("Lula", "pt");
        } catch (Exception ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
