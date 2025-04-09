# BMEM Layer Architecture

### Made by Diego Pacheco

## Overview
This research repository demonstrates a sophisticated AI agent system with persistent memory capabilities using knowledge graphs. The project explores effective memory management techniques through a Neo4j graph database backend and a Java-based implementation, creating a seamless interaction between users and an AI assistant that can "dig into" its memories.

## Research Focus Areas
- Memory persistence and retrieval strategies in conversational agents
- Dynamic behavior adaptation based on accumulated memories
- Effective memory encoding and representation techniques
- Long-term vs. short-term memory management
- Memory contextualization and relevance assessment
- Memory-augmented reasoning and decision making

## Features
- **3D Graph Visualization**: Interactive graph visualization showing memory nodes and relationships
- **Real-time Memory Exploration**: Watch as the AI agent explores its memory graph in real-time
- **WebSocket Communication**: Seamless communication between frontend and backend
- **Multi-Model Support**: Integration with multiple LLM providers (OpenAI, Anthropic, Google, etc.)
- **Neo4j Knowledge Graph**: Persistent storage of agent memories in a graph database
- **Modern UI**: Responsive design with tailored styling and smooth animations

## Technology Stack
- **Backend**: Java, Spring Boot, Spring WebSocket
- **Frontend**: React + Vite, TailwindCSS, Shadcn/UI, React Force Graph
- **Memory Storage**: Neo4j Graph Database
- **Build Tool**: Maven/Gradle
- **LLM Integration**: Java SDK clients for OpenAI, Anthropic, Google, and others

## Installation Guide

### Prerequisites
- Java JDK 17+
- Maven 3.8+ 
- Node.js 16+ (for frontend)
- Neo4j Database (local or cloud instance)

### Backend Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/dpacheco2001/BMemLayerJava.git
   cd BMemLayerJava