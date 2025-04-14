package xyz.neruxov.advertee.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import xyz.neruxov.advertee.data.client.model.Client
import xyz.neruxov.advertee.service.ClientService
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/clients")
class ClientController(private val clientService: ClientService) {

    @GetMapping("/{id}")
    fun getClientById(
        @PathVariable id: UUID
    ): Client = clientService.getById(id)

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    fun createClients(
        @RequestBody @Valid clients: List<Client>
    ): List<Client> = clients.map { clientService.create(it) }

}