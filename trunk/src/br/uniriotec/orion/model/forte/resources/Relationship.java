
package br.uniriotec.orion.model.forte.resources;

import java.util.ArrayList;
import java.util.List;


/** Classe representante de um relacionamento em clausula de Horn.
 *
 * @author Felipe
 */
public class Relationship extends ForteResource {
    private String nomeRelacionamento;
    private List<String> primeiroTermo;
    private String segundoTermo;

    @Override
    public String toString(){
        String result = "";
        for(String s : primeiroTermo){
            result += nomeRelacionamento + "(A,B) :- " + s + "(A), " + segundoTermo + "(B); ";
        }
        result = result.substring (0, result.length() - 2);
        return result;
    }

    public List<String> getRelacionamentos(){
        List<String> result = new ArrayList<String>();
        for(String s : primeiroTermo){
            result.add(nomeRelacionamento + "(A,B) :- " + s + "(A), " + segundoTermo + "(B)");
        }
        return result;
    }

    public String getNome() {
        return nomeRelacionamento;
    }

    public void setNome(String nome) {
        this.nomeRelacionamento = nome;
    }

    public List<String> getPrimeiroTermo() {
        return primeiroTermo;
    }

    public void setPrimeiroTermo(List<String> primeiroTermo) {
        this.primeiroTermo = primeiroTermo;
    }

    public String getSegundoTermo() {
        return segundoTermo;
    }

    public void setSegundoTermo(String segundoTermo) {
        this.segundoTermo = segundoTermo;
    }
}
