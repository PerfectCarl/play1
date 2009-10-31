package controllers

import play._
import play.mvc._
import play.data.validation._
import play.libs._
import play.cache._
import play.db.jpa.Helpers._
 
import models._

object Application extends Actions {
    
    @Before
    private def addDefaults {
        renderArgs.put("blogTitle", Play.configuration.getProperty("blog.title"))
        renderArgs.put("blogBaseline", Play.configuration.getProperty("blog.baseline"))
    }
 
    def index() { 
        val frontPost = jpql("from Post order by postedAt desc").first[Post]
        val olderPosts = jpql("from Post order by postedAt desc").from(1).fetch[Post](10)
        render(frontPost, olderPosts);
    }
    
    def show(id: Long) { 
        val post = jpql("from Post where id = ?", id).first[Post]
        val randomID = Codec.UUID
        render(post, randomID)
    }
    
    def postComment(
        postId: Long, 
        @Required(message="Author is required") author: String, 
        @Required(message="A message is required") content: String, 
        @Required(message="Please type the code") code: String, 
        randomID: String) 
    {
        val post = jpql("from Post where id = ?", postId).first[Post]
        
        Play.id match {            
            case "test" => // skip validation
            case _ => validation.equals(code, Cache.get(randomID)).message("Invalid code. Please type it again")            
        }
        
        if(Validation.hasErrors()) {
            render("@show", post, randomID)
        }
        
        post.addComment(author, content)
        
        flash.success("Thanks for posting %s", author)
        show(postId)
    }
    
    def captcha(id: String) = {
        val captcha = Images.captcha
        val code = captcha.getText("#E4EAFD")
        Cache.set(id, code, "30mn")
        captcha
    }
    
    def listTagged(tag: String) {
        val posts = Post.findTaggedWith(tag)
        render(tag, posts);
    }
 
}