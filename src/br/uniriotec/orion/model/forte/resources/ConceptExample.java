/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.uniriotec.orion.model.forte.resources;

/**Classe para representar exemplos de conceitos, tanto positivos quanto
 * negativos. Exemplos de conceitos possum um único termo.
 *  - Ex.: aluno(João);
 *
 * Dois atributos básicos são considerados:
 *  - conceito: o conceito ao qual se refere o exemplo,
 *  - primeiroTermo: a instancia utilizada para criar o exemplo.
 *
 * @author Felipe
 */
public class ConceptExample implements IExample {
    protected String predicado;
    protected String primeiroTermo;

    public ConceptExample(){};

    public String getPredicado() {
        return predicado;
    }

    public void setPredicado(String predicado) {
        this.predicado = predicado;
    }

    public String getPrimeiroTermo() {
        return primeiroTermo;
    }

    public void setPrimeiroTermo(String primeiroTermo) {
        this.primeiroTermo = primeiroTermo;
    }

    @Override
    public String toString(){
        return predicado + "(" + primeiroTermo + ")";
    }

}
