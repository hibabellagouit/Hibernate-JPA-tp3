package org.example;


import com.example.model.Equipement;
import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.Utilisateur;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.List;

public class App {
    public static void main(String[] args) {
        // Création de l'EntityManagerFactory
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion-reservations");

        try {
            // Test des relations et des opérations en cascade
            System.out.println("\n=== Test des relations et des opérations en cascade ===");
            testRelationsEtCascade(emf);

            // Test de la suppression orpheline
            System.out.println("\n=== Test de la suppression orpheline ===");
            testSuppressionOrpheline(emf);

            // Test de la relation ManyToMany avec Équipement
            System.out.println("\n=== Test de la relation ManyToMany avec Équipement ===");
            testRelationManyToMany(emf);

        } finally {
            // Fermeture de l'EntityManagerFactory
            emf.close();
        }
    }

    private static void testRelationsEtCascade(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            // Création des entités
            System.out.println("Création des entités...");

            // Création d'un utilisateur
            Utilisateur utilisateur = new Utilisateur("Dupont", "Jean", "jean.dupont@example.com");

            // Création d'une salle
            Salle salle = new Salle("Salle A101", 30);
            salle.setDescription("Salle de réunion équipée d'un projecteur");

            // Création d'une réservation
            Reservation reservation = new Reservation(
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(1).plusHours(2),
                    "Réunion d'équipe"
            );

            // Établissement des relations
            utilisateur.addReservation(reservation);
            salle.addReservation(reservation);

            // Persistance de l'utilisateur avec cascade sur la réservation
            em.persist(utilisateur);
            em.persist(salle);

            em.getTransaction().commit();
            System.out.println("Entités créées et liées avec succès !");

            // Vérification des entités persistées
            em.clear(); // Vider le contexte de persistance

            System.out.println("\nVérification des entités persistées :");
            Utilisateur utilisateurPersiste = em.find(Utilisateur.class, utilisateur.getId());
            System.out.println("Utilisateur : " + utilisateurPersiste);
            System.out.println("Nombre de réservations : " + utilisateurPersiste.getReservations().size());

            Salle sallePersistee = em.find(Salle.class, salle.getId());
            System.out.println("Salle : " + sallePersistee);
            System.out.println("Nombre de réservations : " + sallePersistee.getReservations().size());

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private static void testSuppressionOrpheline(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            // Création d'un utilisateur avec des réservations
            em.getTransaction().begin();

            Utilisateur utilisateur = new Utilisateur("Martin", "Sophie", "sophie.martin@example.com");

            Salle salle1 = new Salle("Salle B102", 20);
            em.persist(salle1);

            Salle salle2 = new Salle("Salle C103", 15);
            em.persist(salle2);

            // Création de deux réservations
            Reservation reservation1 = new Reservation(
                    LocalDateTime.now().plusDays(2),
                    LocalDateTime.now().plusDays(2).plusHours(1),
                    "Entretien"
            );

            Reservation reservation2 = new Reservation(
                    LocalDateTime.now().plusDays(3),
                    LocalDateTime.now().plusDays(3).plusHours(2),
                    "Formation"
            );

            // Établissement des relations
            utilisateur.addReservation(reservation1);
            utilisateur.addReservation(reservation2);
            salle1.addReservation(reservation1);
            salle2.addReservation(reservation2);

            em.persist(utilisateur);

            em.getTransaction().commit();
            System.out.println("Utilisateur avec deux réservations créé !");

            // Suppression d'une réservation (test de la suppression orpheline)
            em.getTransaction().begin();

            Utilisateur utilisateurAModifier = em.find(Utilisateur.class, utilisateur.getId());
            System.out.println("Nombre de réservations avant suppression : " + utilisateurAModifier.getReservations().size());

            // Suppression de la première réservation (sera supprimée grâce à orphanRemoval=true)
            Reservation reservationASupprimer = utilisateurAModifier.getReservations().get(0);
            utilisateurAModifier.removeReservation(reservationASupprimer);

            em.getTransaction().commit();

            // Vérification de la suppression
            em.clear();
            Utilisateur utilisateurApresModification = em.find(Utilisateur.class, utilisateur.getId());
            System.out.println("Nombre de réservations après suppression : " + utilisateurApresModification.getReservations().size());

            // Vérification que la réservation a bien été supprimée de la base de données
            Long reservationId = reservationASupprimer.getId();
            Reservation reservationSupprimee = em.find(Reservation.class, reservationId);
            System.out.println("La réservation existe-t-elle encore ? " + (reservationSupprimee != null));

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private static void testRelationManyToMany(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            // Création des équipements
            Equipement projecteur = new Equipement("Projecteur", "Projecteur HD");
            Equipement ecran = new Equipement("Écran interactif", "Écran tactile 65 pouces");
            Equipement visioconference = new Equipement("Système de visioconférence", "Système complet avec caméra HD");

            // Création des salles
            Salle salleReunion = new Salle("Salle de réunion D104", 25);
            Salle salleFormation = new Salle("Salle de formation E205", 40);

            // Ajout des équipements aux salles
            salleReunion.addEquipement(projecteur);
            salleReunion.addEquipement(visioconference);

            salleFormation.addEquipement(projecteur);
            salleFormation.addEquipement(ecran);

            // Persistance des salles (les équipements sont persistés en cascade)
            em.persist(salleReunion);
            em.persist(salleFormation);

            em.getTransaction().commit();
            System.out.println("Salles et équipements créés avec succès !");

            // Vérification des relations
            em.clear();

            System.out.println("\nVérification des relations ManyToMany :");

            // Récupération des salles
            Salle salleReunionPersistee = em.find(Salle.class, salleReunion.getId());
            System.out.println("Salle : " + salleReunionPersistee.getNom());
            System.out.println("Équipements :");
            for (Equipement equipement : salleReunionPersistee.getEquipements()) {
                System.out.println("- " + equipement.getNom());
            }

            Salle salleFormationPersistee = em.find(Salle.class, salleFormation.getId());
            System.out.println("\nSalle : " + salleFormationPersistee.getNom());
            System.out.println("Équipements :");
            for (Equipement equipement : salleFormationPersistee.getEquipements()) {
                System.out.println("- " + equipement.getNom());
            }

            // Récupération d'un équipement et affichage des salles associées
            Equipement projecteurPersiste = em.createQuery(
                            "SELECT e FROM Equipement e WHERE e.nom = :nom", Equipement.class)
                    .setParameter("nom", "Projecteur")
                    .getSingleResult();

            System.out.println("\nÉquipement : " + projecteurPersiste.getNom());
            System.out.println("Salles équipées :");
            for (Salle salle : projecteurPersiste.getSalles()) {
                System.out.println("- " + salle.getNom());
            }

            // Test de suppression d'un équipement d'une salle
            em.getTransaction().begin();

            salleReunionPersistee.removeEquipement(projecteurPersiste);

            em.getTransaction().commit();

            // Vérification après suppression
            em.clear();

            Salle salleApresModification = em.find(Salle.class, salleReunion.getId());
            System.out.println("\nSalle après suppression d'un équipement : " + salleApresModification.getNom());
            System.out.println("Équipements restants :");
            for (Equipement equipement : salleApresModification.getEquipements()) {
                System.out.println("- " + equipement.getNom());
            }

            // Vérification que l'équipement existe toujours
            Equipement projecteurApresModification = em.find(Equipement.class, projecteurPersiste.getId());
            System.out.println("\nL'équipement existe-t-il encore ? " + (projecteurApresModification != null));

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}