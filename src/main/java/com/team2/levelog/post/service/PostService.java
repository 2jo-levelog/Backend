package com.team2.levelog.post.service;

import com.team2.levelog.comment.dto.CommentResponseDto;
import com.team2.levelog.comment.entity.Comment;
import com.team2.levelog.comment.repository.CommentRepository;
import com.team2.levelog.global.GlobalResponse.CustomException;
import com.team2.levelog.global.GlobalResponse.code.ErrorCode;
import com.team2.levelog.image.entity.Image;
import com.team2.levelog.image.repository.ImageRepository;
import com.team2.levelog.post.dto.PostLikesResponseDto;
import com.team2.levelog.post.dto.*;
import com.team2.levelog.post.entity.Likes;
import com.team2.levelog.post.repository.LikesRepository;
import com.team2.levelog.post.entity.Post;
import com.team2.levelog.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.team2.levelog.user.entity.User;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

// 1. 기능      :   Post 관련 모든 비즈니스 로직
// 2. 작성자     :   홍윤재
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final LikesRepository likesRepository;
    private final ImageRepository imageRepository;

    //게시글 생성하기
    @Transactional
    public PostResponseDto addPost(PostRequestDto productRequestDto, User user, List<String> imageFiles){

        // 저장소에 입력 받은 데이터 저장 // save()때문에 @Transactional 을 사용하지 않아도 됨
        Post post = postRepository.save(new Post(productRequestDto,user));
        // CommentResponseDto 객체 리스트형으로 선언
        List<CommentResponseDto> commentResponseDtoList = new ArrayList<>();

        // post 객체에 담긴 데이터를 하나씩 CommentResponseDto에 넣어서 초기화 후 commentResponseDtoList에 추가
        for (Comment comment : post.getCommentList()) {
            commentResponseDtoList.add(new CommentResponseDto(comment));
        }
        // db에 url 저장
        List<String> imageList = new ArrayList<>();
        for(String imageFile : imageFiles) {
            Image image = new Image(imageFile, post);
            imageRepository.save(image);
            imageList.add(image.getImageFile());
        }
        return new PostResponseDto(post ,commentResponseDtoList);
    }

    // 메인페이지 게시글 보기
    @Transactional
    public Page<PostMainPageDto> getMainPage(Pageable pageable){
        // DB에 저장된 모든 데이터를 Page 형식으로 postList에 저장
        Page<Post> postList = postRepository.findAll(pageable);
        // Dto 리스트 미리 선언
        List<PostMainPageDto> postMainPageDtoList = new ArrayList<>();

        for(Post post : postList){
            // postList에 담긴 데이터들을 하나씩 Dto 리스트에 담아줌
            List<ImageResponseDto> imageResponseDtoList = new ArrayList<>();
            for (Image image : post.getImages()) {
                // post에 담긴 이미지들을 image 객체에 하나씩 담아줌
                imageResponseDtoList.add(new ImageResponseDto(image));
            }
            // post와 imageResponseDtoList 객체를 postMainPageDtoList에 더해줌
            postMainPageDtoList.add(new PostMainPageDto(post, imageResponseDtoList));
        }

        // 리스트형 객체인 postMainPageDtoList을 페이지형으로 변환
        final Page<PostMainPageDto> page = new PageImpl<>(postMainPageDtoList);
        return page;
    }

    // 개인 블로그 게시글 전체 보기
    public Page<PostBlogDto> getPosts(String userNickname, Pageable pageable) {
        // DB에서 유저 닉네임으로 찾는 데이터를 postList 객체에 저장
        Page<Post> postList = postRepository.findAllByNickname(userNickname, pageable);
        // Dto 리스트 미리 선언
        List<PostBlogDto> postBlogDtoList = new ArrayList<>();

        // postList에 담긴 데이터들 하나씩 빼서 post에 담아줌
        for (Post post : postList) {
            List<ImageResponseDto> imageResponseDtoList = new ArrayList<>();
            // post 객체에 담긴 이미지 데이터를 배서 image 객체에 저장
            for (Image image : post.getImages()) {
                // Dto 리스트에 Dto로 감싸서 추가
                imageResponseDtoList.add(new ImageResponseDto(image));
            }
            // post와 imageResponseDtoList 객체를 postBlogDtoList에 더해줌
            postBlogDtoList.add(new PostBlogDto(post, imageResponseDtoList));
        }

        // 리스트형 객체인 postBlogDtoList를 페이지형으로 변환
        final Page<PostBlogDto> page = new PageImpl<>(postBlogDtoList);
        return page;
    }

    //개인 블로그 게시글 상세페이지
    public PostResponseDto getPost(Long id) {
        // 포스트 ID로 DB에서 검색 후 데이터 post 객체에 저장
        Post post = postRepository.findById(id).orElseThrow(
                // 못 찾았으면 에러 처리
                ()-> new CustomException(ErrorCode.POST_NOT_FOUND)
        );

        // 이미지를 리스트로 담을 DtoList 미리 선언
        List<ImageResponseDto> imageResponseDtoList = new ArrayList<>();

        // post 객체에서 이미지 하나씩 빼서 DtoList에 후가
        for (Image image : post.getImages()) {
            imageResponseDtoList.add(new ImageResponseDto(image));
        }

        // 댓글을 담을 DtoList 미리 선언
        List<CommentResponseDto> commentResponseDtoList = new ArrayList<>();
        // post 객체에서 댓글을 빼서 comment에 추가
        for (Comment comment : post.getCommentList()) {
            // 대댓글을 담을 DtoList 선언
            List<CommentResponseDto> childCommentList = new ArrayList<>();
            // 댓글의 부모가 없다면 (일반 댓글이라면) commnet 객체에서 대댓글들을 하나씩 빼서 parentComment 객체에 넣어줌
            if(comment.getChildren()==null){
                for (Comment parentComment : comment.getParent()){
                    // parentComment 객체에서 찾은 사용자의 아이디와 전달받은 id값이 일치하면 childCommentList에 Dto로 감싸서 넣어줌
                    if (id.equals(parentComment.getPost().getId())) {
                        childCommentList.add(new CommentResponseDto(parentComment));
                    }
                }
                // commentResponseDtoList에 데이터 추가
                commentResponseDtoList.add(new CommentResponseDto(comment,childCommentList));
            }
        }
        return new PostResponseDto(post, imageResponseDtoList, commentResponseDtoList);
    }

    // 포스트 수정
    @Transactional
    public PostResponseDto updatePost(Long id, PostRequestDto postRequestDto, User user) {
        // DB에서 Id로 검색한 데이터를 post 객체에 저장
        Post post = postRepository.findById(id).orElseThrow(
                // 없으면 예외 처리
                ()-> new CustomException(ErrorCode.POST_NOT_FOUND)
        );

        // post의 작성자 Id와 현재 사용자의 Id가 같다면 포스트 업데이트 메소드를 사용해서 업데이트 해줌
        if(user.getId().equals(post.getUser().getId())) {
            post.update(postRequestDto);
            return new PostResponseDto(post);
        } else {
            // 그 외에는 전부 예외 처리
            throw new CustomException(ErrorCode.NO_ACCESS);
        }
    }

    // 포스트 삭제
    public void deletePost(Long id, User user) {
        // DB에서 id로 검색한 값 post에 저장
        Post post = postRepository.findById(id).orElseThrow(
                // 없으면 예외 처리
                ()-> new CustomException(ErrorCode.POST_NOT_FOUND)
        );
        // 작성자가 맞는지 확인
        if(user.getId().equals(post.getUser().getId())) {
            // 맞으면 데이터 삭제
            postRepository.delete(post);
        }else {
            throw new CustomException(ErrorCode.NO_ACCESS);
        }
    }

    // 포스트 좋아요 기능
    @Transactional
    public PostLikesResponseDto postLike(Long id, User user) {
        // DB에서 id로 검색한 값 post에 저장
        Post post = postRepository.findById(id).orElseThrow(
                // 없으면 예외 처리
                ()-> new CustomException(ErrorCode.POST_NOT_FOUND)
        );
        // post와 user 객체 정보로 데이터 조회 후 있으면 카운트 -1하고 DB에서 데이터 삭제함
        if(likesRepository.findByPostAndUser(post, user).isPresent()) {
            post.update_count(-1);
            likesRepository.deleteByPostAndUser(post, user);
        } else {
            // 그 외에는 카운트 +1하고 데이터 저장
            post.update_count(1);
            likesRepository.save(new Likes(user, post));
        }
        return new PostLikesResponseDto(post.getCount());
    }
}
