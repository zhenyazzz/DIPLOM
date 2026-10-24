package com.portfolio.common.web.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.common.web.support.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@Transactional
class AbstractCrudControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    void crudAndListEndpoints() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Road","status":"ACTIVE","budget":1500.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Road"))
                .andReturn();

        String id = jsonMapper.readTree(created.getResponse().getContentAsString()).get("id").asString();

        mockMvc.perform(get("/api/projects/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budget").value(1500.00));

        mockMvc.perform(put("/api/projects/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Road-2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Road-2"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tunnel","status":"DRAFT","budget":900.00}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc")
                        .param("filter", "status==ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Road-2"));

        mockMvc.perform(get("/api/projects").param("filter", "createdBy==x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_FILTER"))
                .andExpect(jsonPath("$.title").value("Bad Filter"));

        mockMvc.perform(get("/api/projects").param("sort", "createdAt,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_FILTER"));

        mockMvc.perform(delete("/api/projects/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void validationError() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","status":"ACTIVE","budget":0}
                                """))
                .andExpect(status().isBadRequest());
    }
}
