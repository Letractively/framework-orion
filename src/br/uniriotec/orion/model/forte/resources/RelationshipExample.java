/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.uniriotec.orion.model.forte.resources;

/**Classe para representar exemplos de Relacionamentos, tanto positivos quanto
 * negativos. Exemplos de relacionamentos possum dois termos, o DOMAIN e o RANGE.
 * Como a classe é implementada via herança o domain é representado pelo atributo
 * "primeiroTermo" e o range será implementado pelo atributo "segundoTermo".
 *  - Ex.: cursa(João, Informática);
 *
 * Um atributo básico é considerado, os outros são herdados:
 *  - segundoTermo: o valor do RANGE.
 *
 * @author Felipe
 */
public class RelationshipExample extends ConceptExample {
    private String segundoTermo;

    public String getSegundoTermo() {
        return segundoTermo;
    }

    public void setSegundoTermo(String segundoTermo) {
        this.segundoTermo = segundoTermo;
    }

    @Override
    public String toString(){
        return predicado + "(" + primeiroTermo + "," + segundoTermo + ")";
    }

}
