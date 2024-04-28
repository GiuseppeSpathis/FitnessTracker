package com.example.fitnesstracker

class SocialModel {




    private var personList: MutableList<Person> = mutableListOf(  //questo array in futuro non ci dovra' essere
        Person("Mario", "Maschio"),
        Person("Mariossa", "Maschio"),
        Person("Mariossa", "Maschio"),
        Person("Mar", "other"),
        Person("Mariossad", "male"),
        Person("Maria", "Femmina")
    )

    fun getPersonsList(): List<Person>{
        return personList.toList()
    }

    fun updateList(address: String?): Boolean{ //da chiamare dal broadcast receiver
        //si fa la chiamata al database per vedere se esiste quel dispositivo
        //se esiste aggiorna la lista e  ritora true
        if(address == null)
            return false
        return true
    }

    fun filterList(filter: String):  List<Person>{
        val filteredList = personList.filter { person ->
            person.name.contains(filter, ignoreCase = true)
        }
        return filteredList.toList()
    }



}