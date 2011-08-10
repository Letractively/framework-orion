/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.uniriotec.orion.model.forte.resources;

/**Classe para representar um Axioma de classe em um conceito
 *
 * @author Felipe
 */
public class ConceptAxiom extends ForteResource {
    private String nome;
    private String valor;

    @Override
    public String toString(){
        return nome + ": " + valor;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
