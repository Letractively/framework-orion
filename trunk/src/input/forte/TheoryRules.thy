Ex_Jogador(A) :- Jogador(A), naoTitular(A).
Apelido_Time(A) :- Apelido(A), naoApelido_Torcida(A), refereseTime(A,B), Time(B).
Presidente(A) :- Funcionario(A), naoTecnico(A), comandaClube(A,B), Time(B).
Membro_Comissao_Tecnica(A) :- Funcionario(A), naoTecnico(A), naoPresidente(A), trabalhaEm(A,B), Time(B).
Tecnico(A) :- Funcionario(A), trabalhaEm(A,B), Time(B).
Titular(A) :- Jogador(A).
Apelido_Pessoa(A) :- Apelido(A), naoApelido_Torcida(A), naoApelido_Time(A), referesePessoa(A,B), Pessoa(B).
Apelido_Torcida(A) :- Apelido(A), refereseTorcida(A,B), Torcida(B).
Funcionario(A) :- Pessoa(A).
Jogador(A) :- Pessoa(A), jogaEmTime(A,B), Time(B).
Torcedor(A) :- Pessoa(A), torcePorTime(A,B), Time(B).
