
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
            result += nomeRelacionamento + "(a,b) :- " + s + "(a), " + segundoTermo + "(b); ";
        }
        result = result.substring (0, result.length() - 2);
        return result;
    }
    
    /**
     * retorna todos as string que representam relacionamentos para o FORTE.
     * Um relacionamento é baseado em um ObjetctProperty da ontologia, que tem apenas um domain,
     * mas pode possuir multiplos Ranges, desta forma, cada regra gerada representa um par
     * Range+Domain.
     * 
     * @return List<String>
     */
    public List<String> getRelacionamentos(){
        List<String> result = new ArrayList<String>();
        for(String s : primeiroTermo){
            result.add(nomeRelacionamento + "(a,b) :- " + s + "(a), " + segundoTermo + "(b)");
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
