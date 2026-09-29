import Image from "next/image";

type Props = {
  src: string | null;
  alt: string;
  /** 화면 너비별로 몇 픽셀로 그릴지. 브라우저가 이 값으로 받을 파일을 고른다 */
  sizes: string;
};

// 사진이 없는 상품은 글자로 자리를 채운다. 감싸는 쪽에 relative 와 크기가 있어야 한다
export function ProductThumb({ src, alt, sizes }: Props) {
  if (src === null) {
    return <span className="text-subtle text-xs">상품 이미지</span>;
  }

  return (
    <Image src={src} alt={alt} fill sizes={sizes} className="rounded-[inherit] object-cover" />
  );
}
