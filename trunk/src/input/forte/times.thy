apelido_Time(A) :- fdt:naoapelido_Pessoa(A), fdt:naoapelido_Torcida(A), fdt:refereseTime(A,B), time(B).
presidente(A) :- fdt:naomembro_Comissao_Tecnica(A), fdt:naotecnico(A), fdt:comandaClube(A,B), time(B).
torcida(A) :- fdt:naoapelido(A), fdt:naomascote(A), fdt:naoestadio(A), fdt:naotime(A), fdt:naopessoa(A), fdt:naopatrocinador(A), fdt:naotorcedor(A), fdt:torcePorTime(A,B), time(B).
membro_Comissao_Tecnica(A) :- fdt:naotecnico(A), fdt:naopresidente(A), fdt:trabalhaEm(A,B), time(B).
mascote(A) :- fdt:naoapelido(A), fdt:naoestadio(A), fdt:naotorcida(A), fdt:naotime(A), fdt:naopessoa(A), fdt:naopatrocinador(A), fdt:simboliza(A,B), time(B).
time(A) :- fdt:naoapelido(A), fdt:naomascote(A), fdt:naoestadio(A), fdt:naopessoa(A), fdt:naopatrocinador(A), fdt:naotorcida(A).
